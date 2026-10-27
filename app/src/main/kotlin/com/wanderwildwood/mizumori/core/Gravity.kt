package com.wanderwildwood.mizumori.core

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Working out the angles from the accelerometer, in the phone's own frame.
 *
 * The phone is always measured the way it was built: x across the short edge, y up the long
 * one, z out of the screen. Calibration is stored against that frame, so it lands on the
 * same physical edge whichever way the screen happens to be turned. Turning the screen only
 * changes how a reading is drawn (see [ScreenFrame]), never what was measured.
 *
 * This is the same result as asking `SensorManager.getRotationMatrix` for a matrix with a
 * made-up magnetic field and reading its bottom row, without the one pose that call refuses.
 */
object Gravity {

    /** Null in free fall, when there is no gravity to measure. */
    fun reading(gx: Float, gy: Float, gz: Float): Reading? {
        val length = sqrt(gx * gx + gy * gy + gz * gz)
        if (length < 1e-3f) return null
        val pitch = -Math.toDegrees(asin((gy / length).coerceIn(-1f, 1f).toDouble())).toFloat()
        val roll = Math.toDegrees(atan2(gx.toDouble(), gz.toDouble())).toFloat()
        val flatLength = sqrt(gx * gx + gy * gy)
        val balance = if (flatLength == 0f) 0f
        else Math.toDegrees(asin((gx / flatLength).coerceIn(-1f, 1f).toDouble())).toFloat()
        return Reading(Orientation.of(pitch, roll), pitch, roll, balance)
    }
}

/**
 * Where a reading goes on a screen that may be turned.
 *
 * [rotation] is the display rotation in quarter turns, 0 to 3, the same numbers as
 * `Surface.ROTATION_*`. Every answer is how far the bubble should travel, in degrees, as the
 * reader sees the screen: positive is towards the right or the bottom edge, towards the high
 * side, as a real bubble goes.
 */
object ScreenFrame {

    /**
     * The bubble of a phone lying flat, as (towards the right, towards the bottom).
     *
     * In the phone's frame the reading is [Reading.acrossAngle] along x and
     * [Reading.alongAngle] down y; turning the screen swaps and negates them.
     */
    fun flat(reading: Reading, rotation: Int): Pair<Float, Float> {
        val a = reading.acrossAngle
        val p = reading.alongAngle
        return when (rotation and 3) {
            1 -> Pair(-p, a)
            2 -> Pair(-a, -p)
            3 -> Pair(p, -a)
            else -> Pair(a, p)
        }
    }

    /**
     * The single angle of a phone on an edge, positive towards the right of the screen.
     *
     * Upright, the screen is read along the phone's x axis; on its long edge, with the
     * screen turned sideways, along its y axis. The sign follows the screen so the bubble
     * still goes to the high side when the phone is turned either way.
     */
    fun edge(reading: Reading, rotation: Int): Float {
        val r = rotation and 3
        return when (reading.orientation) {
            Orientation.TOP, Orientation.BOTTOM -> if (r == 2) -reading.angle else reading.angle
            Orientation.LEFT, Orientation.RIGHT -> when (r) {
                1 -> -reading.angle
                2 -> -reading.angle
                else -> reading.angle
            }
            Orientation.FLAT -> reading.angle
        }
    }

    /** Whether the display is on its side, which is when the layout puts the vial beside the number. */
    fun isLandscape(rotation: Int): Boolean = (rotation and 1) == 1
}
