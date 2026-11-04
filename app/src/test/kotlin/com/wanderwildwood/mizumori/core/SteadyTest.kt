package com.wanderwildwood.mizumori.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SteadyTest {

    private fun flat(across: Float, along: Float) =
        Reading(Orientation.FLAT, pitch = along, roll = across, balance = 37f)

    /** Feeds [ms] of readings at 16 Hz and counts how often what is shown changed. */
    private fun changes(steady: Steady, startMs: Long, ms: Long, reading: (Long) -> Reading): Pair<Int, Reading> {
        var last: Reading? = null
        var count = 0
        var t = startMs
        while (t < startMs + ms) {
            val shown = steady.offer(reading(t), t)
            if (last != null && shown !== last) count++
            last = shown
            t += 62
        }
        return count to last!!
    }

    @Test
    fun `the first reading is shown at once, rounded to a tenth`() {
        val shown = Steady().offer(flat(0.34f, -1.26f), 0L)
        assertEquals(0.3f, shown.acrossAngle, 1e-6f)
        assertEquals(-1.3f, shown.alongAngle, 1e-6f)
    }

    /**
     * A phone lying still with a few tenths of sensor noise, its true angle right on a rounding
     * boundary, and heavier noise than a phone at rest shows: a few corrections while it settles
     * (the first value shown is one raw sample), then nothing at all for a minute.
     */
    @Test
    fun `noise on a still phone changes nothing once settled`() {
        for (seed in 0 until 10) {
            val steady = Steady()
            val random = Random(seed)
            val noise = { (random.nextFloat() - 0.5f) * 0.5f }
            val (settling, _) = changes(steady, 0L, 10_000L) { flat(0.15f + noise(), 0.05f + noise()) }
            assertTrue("seed $seed settling $settling", settling <= 4)
            val (count, _) = changes(steady, 10_000L, 60_000L) { flat(0.15f + noise(), 0.05f + noise()) }
            assertEquals("seed $seed", 0, count)
        }
    }

    /** A slow creep, a twentieth of a degree a second, still reaches the screen. */
    @Test
    fun `a slow drift is followed`() {
        val steady = Steady()
        val (count, shown) = changes(steady, 0L, 20_000L) { t -> flat(t / 20_000f, 0f) }
        assertTrue("changes $count", count >= 4)
        assertEquals(1f, shown.acrossAngle, 0.2f)
    }

    /** Sitting right on a rounding boundary is the worst case for flicker; still nothing. */
    @Test
    fun `a reading on a rounding boundary does not flicker`() {
        val steady = Steady()
        var flip = false
        val (count, _) = changes(steady, 0L, 10_000L) {
            flip = !flip
            flat(if (flip) 0.149f else 0.151f, 0f)
        }
        assertEquals(0, count)
    }

    @Test
    fun `a real change shows after the hold, not before`() {
        val steady = Steady()
        val first = steady.offer(flat(0f, 0f), 0L)
        // Tilted by a degree: within the hold it stays put.
        val (early, _) = changes(steady, 100L, 400L) { flat(1f, 0f) }
        assertEquals(0, early)
        assertSame(first, steady.offer(flat(1f, 0f), 450L))
        val (late, shown) = changes(steady, 500L, 1500L) { flat(1f, 0f) }
        assertTrue(late >= 1)
        assertEquals(1f, shown.acrossAngle, 0.05f)
    }

    /** A slow, steady tilt updates the screen, but never more than twice a second. */
    @Test
    fun `a slow tilt updates at most twice a second`() {
        val steady = Steady()
        val (count, shown) = changes(steady, 0L, 10_000L) { t -> flat(t / 1000f, 0f) }
        assertTrue("changes $count", count in 5..20)
        assertEquals(10f, shown.acrossAngle, 1.5f)
    }

    /** Lying flat, the edge balance swings about with nothing shown depending on it. */
    @Test
    fun `an angle that is not shown does not cause a redraw`() {
        val steady = Steady()
        val random = Random(9)
        val (count, _) = changes(steady, 0L, 5_000L) {
            Reading(Orientation.FLAT, 0f, 0f, random.nextFloat() * 90f)
        }
        assertEquals(0, count)
    }

    @Test
    fun `turning it onto an edge is shown`() {
        val steady = Steady()
        steady.offer(flat(0f, 0f), 0L)
        val (_, shown) = changes(steady, 62L, 2_000L) { Reading(Orientation.TOP, -90f, 0f, 0.4f) }
        assertEquals(Orientation.TOP, shown.orientation)
        assertEquals(0.4f, shown.angle, 1e-6f)
    }

    @Test
    fun `after a seed the zero holds against noise`() {
        val steady = Steady()
        steady.offer(flat(3f, 3f), 0L)
        val zero = flat(0f, 0f)
        steady.seed(zero, 100L)
        val random = Random(1)
        val (count, shown) = changes(steady, 162L, 5_000L) {
            flat((random.nextFloat() - 0.5f) * 0.3f, (random.nextFloat() - 0.5f) * 0.3f)
        }
        assertEquals(0, count)
        assertSame(zero, shown)
    }
}
