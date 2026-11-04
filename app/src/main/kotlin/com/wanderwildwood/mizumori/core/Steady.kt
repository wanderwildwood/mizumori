package com.wanderwildwood.mizumori.core

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * What the screen shows, decided from the sensor stream: a reading that changes only when the
 * phone has really moved.
 *
 * A phone lying still on a table still reports angles that wander a tenth or two either side
 * of where it is, many times a second. Shown as they come, the number and the bubble twitch
 * all the time, and on E Ink every twitch is a repaint. So:
 *
 * - the angles are smoothed a little, so one stray sample is not a movement;
 * - what is shown is rounded to the tenth of a degree that is printed, bubble included;
 * - a new value is taken only when the smoothed angle is well past the rounding boundary of
 *   the one on the screen ([BAND]: the half tenth plus a margin), so a reading sitting on a
 *   boundary does not flip between two numbers, and only once it has stayed there for [holdMs];
 * - once a value has stood for a moment the band widens ([SETTLED_BAND]), so a phone at rest
 *   stays put even when its angle sits right on a boundary;
 * - and never more often than once every [minIntervalMs].
 *
 * Held still, nothing changes, so nothing is drawn. Kept free of Android so it can be tested.
 */
class Steady(
    private val holdMs: Long = 500L,
    private val minIntervalMs: Long = 500L,
    private val smoothingMs: Float = 250f,
) {
    private var smoothed: Reading? = null
    private var lastSampleAt = 0L
    private var shown: Reading? = null
    private var shownAt = 0L
    private var movedSince: Long? = null

    /** Forget everything, so the next reading is shown at once (after a calibration, say). */
    fun reset() {
        smoothed = null
        shown = null
        movedSince = null
    }

    /**
     * Start from [reading] as though it had just been shown: after a calibration the screen
     * says zero, and the readings that follow are smoothed from there rather than from one
     * raw sample.
     */
    fun seed(reading: Reading, nowMs: Long) {
        smoothed = reading
        lastSampleAt = nowMs
        shown = reading
        shownAt = nowMs
        movedSince = null
    }

    /**
     * Takes one reading at [nowMs] and returns what the screen should show: the same object as
     * last time when nothing has changed enough, a new one when it has.
     */
    fun offer(reading: Reading, nowMs: Long): Reading {
        val s = smooth(reading, nowMs)
        val current = shown
        if (current == null) {
            return show(s, nowMs)
        }
        val band = if (nowMs - shownAt < SETTLE_MS) BAND else SETTLED_BAND
        if (!differs(current, s, band)) {
            movedSince = null
            return current
        }
        val since = movedSince ?: nowMs.also { movedSince = it }
        if (nowMs - since < holdMs || nowMs - shownAt < minIntervalMs) return current
        return show(s, nowMs)
    }

    private fun show(reading: Reading, nowMs: Long): Reading {
        val q = quantised(reading)
        shown = q
        shownAt = nowMs
        movedSince = null
        return q
    }

    /** An exponential average with a fixed time constant, so it behaves the same at any sensor rate. */
    private fun smooth(reading: Reading, nowMs: Long): Reading {
        val previous = smoothed
        val dt = (nowMs - lastSampleAt).coerceAtLeast(0L)
        lastSampleAt = nowMs
        // A new way of holding it, or an angle that wrapped round past 180, starts afresh:
        // averaging across either would pass through readings that never happened.
        if (previous == null || previous.orientation != reading.orientation ||
            abs(previous.pitch - reading.pitch) > 90f ||
            abs(previous.roll - reading.roll) > 90f ||
            abs(previous.balance - reading.balance) > 90f
        ) {
            return reading.also { smoothed = it }
        }
        val a = 1f - exp(-dt / smoothingMs)
        return Reading(
            orientation = reading.orientation,
            pitch = previous.pitch + a * (reading.pitch - previous.pitch),
            roll = previous.roll + a * (reading.roll - previous.roll),
            balance = previous.balance + a * (reading.balance - previous.balance),
        ).also { smoothed = it }
    }

    /**
     * Whether [now] is far enough from what is [shown] to be worth showing.
     *
     * Only the angles this way of holding it actually displays count: lying flat, the edge
     * balance swings wildly with nothing on the screen depending on it.
     */
    private fun differs(shown: Reading, now: Reading, band: Float): Boolean {
        if (shown.orientation != now.orientation) return true
        return when (now.orientation) {
            Orientation.FLAT -> abs(now.acrossAngle - shown.acrossAngle) >= band ||
                abs(now.alongAngle - shown.alongAngle) >= band
            Orientation.TOP, Orientation.BOTTOM -> abs(now.balance - shown.balance) >= band
            Orientation.LEFT, Orientation.RIGHT -> abs(now.alongAngle - shown.alongAngle) >= band
        }
    }

    companion object {
        /**
         * How far from the shown value the smoothed angle has to be before it counts: the
         * half tenth at which the rounding would change, plus 0.03 of hysteresis. Not a full
         * tenth, or an angle closing in slowly on the next tenth would never quite get there
         * and the screen would sit one tenth short. A change, when it is shown, is always a
         * whole tenth or more.
         */
        const val BAND = 0.08f

        /**
         * Once a value has stood for [SETTLE_MS], the band widens to this. A phone at rest whose
         * true angle sits right on a boundary would otherwise still flip now and then, whenever
         * the noise happened to lean one way for half a second; at rest the screen should not
         * move at all. A real slow drift still crosses it and is shown.
         */
        const val SETTLED_BAND = 0.15f
        const val SETTLE_MS = 1500L

        fun quantised(reading: Reading): Reading = Reading(
            orientation = reading.orientation,
            pitch = round(reading.pitch),
            roll = round(reading.roll),
            balance = round(reading.balance),
        )

        private fun round(value: Float): Float {
            val r = (value * 10f).roundToInt() / 10f
            return if (r == 0f) 0f else r
        }
    }
}
