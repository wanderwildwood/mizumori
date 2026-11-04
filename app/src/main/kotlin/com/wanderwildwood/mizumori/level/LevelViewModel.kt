package com.wanderwildwood.mizumori.level

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanderwildwood.mizumori.core.Display
import com.wanderwildwood.mizumori.core.Orientation
import com.wanderwildwood.mizumori.core.Reading
import com.wanderwildwood.mizumori.core.Steady
import com.wanderwildwood.mizumori.device.hasAccelerometer
import com.wanderwildwood.mizumori.device.readings
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** How many degrees off still counts as level. A spirit level's vial is about this. */
const val TOLERANCE_DEGREES = 0.2f

data class LevelState(
    val reading: Reading? = null,
    val display: Display = Display.DEGREES,
    val lockOrientation: Boolean = false,
    val soundWhenLevel: Boolean = false,
    val isCalibrated: Boolean = false,
    val hasSensor: Boolean = true,
    /** Set for one recomposition after a calibration, so the screen can say so. */
    val justCalibrated: Boolean = false,
) {
    val isLevel: Boolean
        get() = reading?.orientation
            ?.isLevel(reading.pitch, reading.roll, reading.balance, TOLERANCE_DEGREES)
            ?: false
}

class LevelViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = Preferences(application)

    private val _state = MutableStateFlow(
        LevelState(
            display = preferences.display,
            lockOrientation = preferences.lockOrientation,
            soundWhenLevel = preferences.soundWhenLevel,
            isCalibrated = preferences.isCalibrated,
            hasSensor = hasAccelerometer(application),
        )
    )
    val state: StateFlow<LevelState> = _state.asStateFlow()

    private var job: Job? = null

    private val steady = Steady()

    /** The last raw reading, kept so calibrate can zero against what is on the screen now. */
    private var raw: Reading? = null

    /** The orientation being measured while the lock is on. */
    private var locked: Orientation? = null

    fun start() {
        job?.cancel()
        val context = getApplication<Application>()
        if (!hasAccelerometer(context)) {
            _state.update { it.copy(hasSensor = false) }
            return
        }

        steady.reset()
        _state.update {
            it.copy(
                display = preferences.display,
                lockOrientation = preferences.lockOrientation,
                soundWhenLevel = preferences.soundWhenLevel,
                isCalibrated = preferences.isCalibrated,
            )
        }

        job = viewModelScope.launch {
            readings(context)
                .map { reading ->
                    raw = reading

                    val orientation = if (preferences.lockOrientation) {
                        locked ?: reading.orientation.also { locked = it }
                    } else {
                        locked = null
                        reading.orientation
                    }

                    val (pitch, roll, balance) = preferences.calibration(orientation)
                    Reading(
                        orientation = orientation,
                        pitch = reading.pitch - pitch,
                        roll = reading.roll - roll,
                        balance = reading.balance - balance,
                    )
                }
                // What reaches the screen changes only when the phone has really moved: smoothed,
                // rounded to the tenth that is printed, a full tenth away for half a second, and
                // at most twice a second. Held still, the same object comes back every time and
                // the state below does not change, so nothing is drawn (see [Steady]).
                .map { reading -> steady.offer(reading, SystemClock.elapsedRealtime()) }
                .distinctUntilChanged()
                .collect { reading ->
                    _state.update {
                        if (it.reading == reading && !it.justCalibrated) it
                        else it.copy(reading = reading, justCalibrated = false)
                    }
                }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    fun nextDisplay() {
        preferences.display = preferences.display.next()
        _state.update { it.copy(display = preferences.display) }
    }

    fun toggleLock() {
        preferences.lockOrientation = !preferences.lockOrientation
        locked = if (preferences.lockOrientation) raw?.orientation else null
        _state.update { it.copy(lockOrientation = preferences.lockOrientation) }
    }

    fun toggleSound() {
        preferences.soundWhenLevel = !preferences.soundWhenLevel
        _state.update { it.copy(soundWhenLevel = preferences.soundWhenLevel) }
    }

    /**
     * Call this with the phone on a surface you have some other reason to trust, and
     * whatever it reads now becomes zero for this way of holding it.
     */
    fun calibrate() {
        val current = raw ?: return
        preferences.calibrate(current)
        // The readings now arrive relative to a new zero; the smoothing starts over from it.
        val zero = Reading(current.orientation, 0f, 0f, 0f)
        steady.seed(zero, SystemClock.elapsedRealtime())
        _state.update {
            it.copy(
                reading = zero,
                isCalibrated = true,
                justCalibrated = true,
            )
        }
    }

    /** Whether pressing Calibrate now would replace a zero saved for this way of holding it. */
    fun hasCalibrationHere(): Boolean {
        val orientation = _state.value.reading?.orientation ?: raw?.orientation ?: return false
        return preferences.isCalibrated(orientation)
    }

    fun resetCalibration() {
        preferences.resetCalibration()
        // The zero just moved; show the uncorrected reading straight away, not after the hold.
        steady.reset()
        _state.update { it.copy(isCalibrated = false, justCalibrated = false) }
    }
}
