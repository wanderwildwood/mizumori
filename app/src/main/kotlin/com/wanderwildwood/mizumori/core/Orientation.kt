package com.wanderwildwood.mizumori.core

import kotlin.math.abs

/**
 * Which way the phone is being held, which decides what a level even means.
 *
 * Laid flat it is a two-axis instrument and both pitch and roll matter. Stood on any of
 * its four edges it is a one-axis instrument, and only the angle along that edge does.
 *
 * From Level by Antoine Vianey (2014), GPL-3.0-or-later, by way of woheller69's fork.
 */
enum class Orientation {
    /** Face up on a table. Two axes. */
    FLAT,

    /** Standing on its bottom edge. */
    TOP,

    /** Standing on its top edge. */
    BOTTOM,

    /** Standing on its left edge. */
    RIGHT,

    /** Standing on its right edge. */
    LEFT,
    ;

    val isFlat: Boolean get() = this == FLAT

    /**
     * Whether the reading counts as level, within [tolerance] degrees.
     *
     * Held on edge the phone is level when its face is vertical, which is a pitch of
     * either 0 or 180 — upside down is still level, and a level that only agreed one way
     * up would be wrong half the time you picked it up.
     */
    fun isLevel(pitch: Float, roll: Float, balance: Float, tolerance: Float): Boolean {
        val t = if (tolerance < 0.2f) 0.2f else tolerance
        return when (this) {
            TOP, BOTTOM -> abs(balance) <= t
            FLAT -> abs(roll) <= t && (abs(pitch) <= t || abs(pitch) >= 180 - t)
            LEFT, RIGHT -> abs(pitch) <= t || abs(pitch) >= 180 - t
        }
    }

    companion object {
        /** Which way up the phone is, from the angles themselves. */
        fun of(pitch: Float, roll: Float): Orientation = when {
            pitch < -45 && pitch > -135 -> TOP
            pitch > 45 && pitch < 135 -> BOTTOM
            roll > 45 -> RIGHT
            roll < -45 -> LEFT
            else -> FLAT
        }
    }
}

/** One reading, already corrected for calibration. */
data class Reading(
    val orientation: Orientation,
    val pitch: Float,
    val roll: Float,
    val balance: Float,
) {
    /**
     * The angle this orientation is actually measuring, as a signed number of degrees
     * away from level.
     *
     * On edge that is the balance or the pitch depending on which edge; flat, it is the
     * pair, and this returns the larger of the two so that a single "how far off" figure
     * means the worst axis rather than an average that hides one of them.
     */
    val angle: Float
        get() = when (orientation) {
            Orientation.TOP, Orientation.BOTTOM -> balance
            Orientation.LEFT, Orientation.RIGHT -> normalisePitch(pitch)
            Orientation.FLAT -> if (abs(roll) >= abs(normalisePitch(pitch))) roll
            else normalisePitch(pitch)
        }

    /** Flat, the two axes separately: along the short edge, then the long one. */
    val acrossAngle: Float get() = roll
    val alongAngle: Float get() = normalisePitch(pitch)

    private fun normalisePitch(value: Float): Float = when {
        value > 90 -> value - 180
        value < -90 -> value + 180
        else -> value
    }
}

/** How the angle is said. */
enum class Display {
    /** Degrees from level, which is what a carpenter means. */
    DEGREES,

    /**
     * Rise over run as a percentage, which is what a fall or a ramp is specified in.
     * A 1-in-80 drainage fall is 1.25%, and saying that in degrees (0.72°) helps nobody.
     */
    PERCENT,
    ;

    fun next(): Display = entries[(ordinal + 1) % entries.size]
}
