package com.wanderwildwood.mizumori.core

import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Turning an angle into the words on the screen.
 *
 * Kept apart from the drawing so it can be tested without a phone in a particular
 * attitude, which is the only way any of this is checkable at all.
 */
object Angles {

    /** Degrees from level as a percentage grade: rise over run. */
    fun toPercent(degrees: Float): Float =
        (tan(Math.toRadians(degrees.toDouble())) * 100.0).toFloat()

    /**
     * The reading as it is written, to one decimal place.
     *
     * One decimal and no more. The accelerometer in a phone is not a machinist's level,
     * and a second decimal would be inventing precision the hardware does not have —
     * it would also mean the last digit never stops moving, which on a panel that takes
     * a moment to redraw is a screen that is never still.
     */
    fun format(degrees: Float, display: Display): String = when (display) {
        Display.DEGREES -> "%.1f°".format(clean(degrees))
        Display.PERCENT -> "%.1f%%".format(clean(toPercent(degrees)))
    }

    /**
     * Rounded to the tenth that will actually be shown.
     *
     * The screen is only asked to redraw when this changes. The sensor reports a new
     * value many times a second and almost all of them would print identically; on
     * E Ink, repainting for a digit that did not move is the whole difference between
     * an instrument and a flickering mess.
     */
    fun quantise(degrees: Float): Int = (clean(degrees) * 10f).roundToInt()

    /**
     * Negative zero is still zero.
     *
     * A tiny negative reading formats as "-0.0°", which reads as though it means
     * something. It does not.
     */
    private fun clean(value: Float): Float {
        val rounded = (value * 10f).roundToInt() / 10f
        return if (rounded == 0f) 0f else rounded
    }

    /**
     * How far the bubble sits from the middle, as a fraction from -1 to 1.
     *
     * [range] is the angle at which the bubble reaches the end of its travel. Beyond
     * that it stops at the end rather than disappearing, the way a real one does when
     * it runs up against the glass.
     */
    fun bubbleOffset(degrees: Float, range: Float): Float =
        (degrees / range).coerceIn(-1f, 1f)

    /** Whether a reading is within tolerance, for the one thing the screen shouts about. */
    fun isLevel(degrees: Float, tolerance: Float): Boolean = abs(degrees) <= tolerance
}
