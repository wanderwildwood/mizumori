package com.wanderwildwood.mizumori.level

import android.content.Context
import com.wanderwildwood.mizumori.core.Display
import com.wanderwildwood.mizumori.core.Orientation
import com.wanderwildwood.mizumori.core.Reading

/**
 * What is set, and what has been calibrated.
 *
 * Calibration is stored per orientation, not once. A phone rarely sits at exactly the
 * same angle on its back as on its edge — the camera bump alone sees to that — so one
 * offset applied to all five ways of holding it would correct one and spoil four.
 */
class Preferences(context: Context) {

    private val store = context.getSharedPreferences("level", Context.MODE_PRIVATE)

    var display: Display
        get() {
            val stored = store.getString(DISPLAY, null) ?: return Display.DEGREES
            return Display.entries.firstOrNull { it.name == stored } ?: Display.DEGREES
        }
        set(value) = store.edit().putString(DISPLAY, value.name).apply()

    /** Keep measuring the way it is being held now, even if it is then turned. */
    var lockOrientation: Boolean
        get() = store.getBoolean(LOCK, false)
        set(value) = store.edit().putBoolean(LOCK, value).apply()

    /** Sound when it comes level, for when the surface is where you cannot see the screen. */
    var soundWhenLevel: Boolean
        get() = store.getBoolean(SOUND, false)
        set(value) = store.edit().putBoolean(SOUND, value).apply()

    /** Whether anything has been calibrated at all, which the settings row reports. */
    val isCalibrated: Boolean
        get() = Orientation.entries.any {
            store.contains(key(PITCH, it)) || store.contains(key(ROLL, it)) ||
                store.contains(key(BALANCE, it))
        }

    fun calibration(orientation: Orientation): Triple<Float, Float, Float> = Triple(
        store.getFloat(key(PITCH, orientation), 0f),
        store.getFloat(key(ROLL, orientation), 0f),
        store.getFloat(key(BALANCE, orientation), 0f),
    )

    /** Take the reading as it stands to be the new zero for this orientation. */
    fun calibrate(reading: Reading) {
        store.edit()
            .putFloat(key(PITCH, reading.orientation), reading.pitch)
            .putFloat(key(ROLL, reading.orientation), reading.roll)
            .putFloat(key(BALANCE, reading.orientation), reading.balance)
            .apply()
    }

    /** Back to the factory, for all five orientations at once. */
    fun resetCalibration() {
        val editor = store.edit()
        Orientation.entries.forEach {
            editor.remove(key(PITCH, it)).remove(key(ROLL, it)).remove(key(BALANCE, it))
        }
        editor.apply()
    }

    private fun key(prefix: String, orientation: Orientation) = "$prefix.${orientation.name}"

    private companion object {
        const val DISPLAY = "display"
        const val LOCK = "lock"
        const val SOUND = "sound"
        const val PITCH = "pitch"
        const val ROLL = "roll"
        const val BALANCE = "balance"
    }
}
