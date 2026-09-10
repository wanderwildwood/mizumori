package com.wanderwildwood.mizumori.level

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wanderwildwood.mizumori.core.Angles
import com.wanderwildwood.mizumori.core.Display
import com.wanderwildwood.mizumori.core.Orientation
import com.wanderwildwood.mizumori.core.Reading
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

    /** The last raw reading, kept so calibrate can zero against what is on the screen now. */
    private var raw: Reading? = null

    /** The orientation being measured while the lock is on. */
    private var locked: Orientation? = null

    fun start(displayRotation: Int) {
        job?.cancel()
        val context = getApplication<Application>()
        if (!hasAccelerometer(context)) {
            _state.update { it.copy(hasSensor = false) }
            return
        }

        _state.update {
            it.copy(
                display = preferences.display,
                lockOrientation = preferences.lockOrientation,
                soundWhenLevel = preferences.soundWhenLevel,
                isCalibrated = preferences.isCalibrated,
            )
        }

        job = viewModelScope.launch {
            readings(context, displayRotation)
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
                // The screen redraws only when a shown digit actually changes. Everything
                // upstream of this runs many times a second; this is what stops E Ink
                // being asked to repaint an identical number over and over.
                .distinctUntilChanged { old, new ->
                    old.orientation == new.orientation &&
                        Angles.quantise(old.angle) == Angles.quantise(new.angle) &&
                        Angles.quantise(old.acrossAngle) == Angles.quantise(new.acrossAngle) &&
                        Angles.quantise(old.alongAngle) == Angles.quantise(new.alongAngle)
                }
                .collect { reading ->
                    _state.update { it.copy(reading = reading, justCalibrated = false) }
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
        _state.update {
            it.copy(
                reading = Reading(current.orientation, 0f, 0f, 0f),
                isCalibrated = true,
                justCalibrated = true,
            )
        }
    }

    fun resetCalibration() {
        preferences.resetCalibration()
        _state.update { it.copy(isCalibrated = false, justCalibrated = false) }
    }
}
