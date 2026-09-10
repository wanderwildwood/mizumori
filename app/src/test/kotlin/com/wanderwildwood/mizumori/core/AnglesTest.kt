package com.wanderwildwood.mizumori.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PercentTest {

    @Test
    fun `level is zero grade`() {
        assertEquals(0f, Angles.toPercent(0f), 1e-4f)
    }

    /** 45 degrees is one in one, which is 100%. */
    @Test
    fun `forty five degrees is one hundred percent`() {
        assertEquals(100f, Angles.toPercent(45f), 1e-3f)
    }

    /** A 1-in-80 drainage fall, the figure a plumber actually works to, is 1.25%. */
    @Test
    fun `a one in eighty fall is one and a quarter percent`() {
        val degrees = Math.toDegrees(Math.atan(1.0 / 80.0)).toFloat()
        assertEquals(1.25f, Angles.toPercent(degrees), 0.01f)
    }

    @Test
    fun `a fall the other way is negative`() {
        assertTrue(Angles.toPercent(-3f) < 0)
    }
}

class FormatTest {

    @Test
    fun `degrees carry the degree sign and one decimal`() {
        assertEquals("2.4°", Angles.format(2.44f, Display.DEGREES))
    }

    @Test
    fun `percent carries the percent sign`() {
        assertEquals("100.0%", Angles.format(45f, Display.PERCENT))
    }

    /**
     * A hair below zero must not print as "-0.0°".
     *
     * It reads as a direction the reading does not have, and on a level — where the whole
     * question is which side of zero you are on — that is worse than it sounds.
     */
    @Test
    fun `a tiny negative reading is not minus zero`() {
        assertEquals("0.0°", Angles.format(-0.01f, Display.DEGREES))
        assertEquals("0.0°", Angles.format(-0.04f, Display.DEGREES))
    }

    @Test
    fun `a real negative keeps its sign`() {
        assertEquals("-1.2°", Angles.format(-1.24f, Display.DEGREES))
    }

    @Test
    fun `rounding goes to the nearest tenth, not toward zero`() {
        assertEquals("2.5°", Angles.format(2.46f, Display.DEGREES))
    }
}

class QuantiseTest {

    /**
     * The quantiser is what stops E Ink repainting. These pin that two readings which
     * would print identically really do compare equal, and that a change of one tenth
     * really does get through.
     */
    @Test
    fun `readings that print the same quantise the same`() {
        assertEquals(Angles.quantise(1.230f), Angles.quantise(1.234f))
    }

    @Test
    fun `a tenth of a degree gets through`() {
        assertTrue(Angles.quantise(1.20f) != Angles.quantise(1.30f))
    }

    @Test
    fun `zero and a hair below zero are the same`() {
        assertEquals(Angles.quantise(0f), Angles.quantise(-0.01f))
    }
}

class BubbleOffsetTest {

    @Test
    fun `level puts the bubble in the middle`() {
        assertEquals(0f, Angles.bubbleOffset(0f, 6f), 1e-6f)
    }

    @Test
    fun `half the range is half way out`() {
        assertEquals(0.5f, Angles.bubbleOffset(3f, 6f), 1e-6f)
    }

    /** Past the end of travel it stops, the way a bubble stops against the glass. */
    @Test
    fun `beyond the range it stops at the end`() {
        assertEquals(1f, Angles.bubbleOffset(90f, 6f), 1e-6f)
        assertEquals(-1f, Angles.bubbleOffset(-90f, 6f), 1e-6f)
    }
}

class OrientationTest {

    @Test
    fun `flat on its back`() {
        assertEquals(Orientation.FLAT, Orientation.of(0f, 0f))
    }

    @Test
    fun `standing upright`() {
        assertEquals(Orientation.TOP, Orientation.of(-90f, 0f))
    }

    @Test
    fun `upside down`() {
        assertEquals(Orientation.BOTTOM, Orientation.of(90f, 0f))
    }

    @Test
    fun `on its left and right edges`() {
        assertEquals(Orientation.RIGHT, Orientation.of(0f, 60f))
        assertEquals(Orientation.LEFT, Orientation.of(0f, -60f))
    }

    @Test
    fun `only flat measures two axes`() {
        assertTrue(Orientation.FLAT.isFlat)
        Orientation.entries.filter { it != Orientation.FLAT }.forEach {
            assertFalse("$it should be a one-axis vial", it.isFlat)
        }
    }

    /** On edge, face-up and face-down are both level. A level must agree either way up. */
    @Test
    fun `on edge, both ways up read level`() {
        assertTrue(Orientation.LEFT.isLevel(0f, 0f, 0f, 0.2f))
        assertTrue(Orientation.LEFT.isLevel(180f, 0f, 0f, 0.2f))
        assertFalse(Orientation.LEFT.isLevel(45f, 0f, 0f, 0.2f))
    }

    @Test
    fun `flat needs both axes level, not just one`() {
        assertTrue(Orientation.FLAT.isLevel(0f, 0f, 0f, 0.2f))
        assertFalse("a level roll must not excuse a tilted pitch",
            Orientation.FLAT.isLevel(5f, 0f, 0f, 0.2f))
        assertFalse("a level pitch must not excuse a tilted roll",
            Orientation.FLAT.isLevel(0f, 5f, 0f, 0.2f))
    }

    /** The tolerance never goes below a fifth of a degree, however it is asked. */
    @Test
    fun `a nonsense tolerance is floored, not honoured`() {
        assertTrue(Orientation.TOP.isLevel(0f, 0f, 0.1f, 0f))
    }
}

class ReadingTest {

    @Test
    fun `on edge the angle is the balance`() {
        val r = Reading(Orientation.TOP, pitch = 12f, roll = 3f, balance = 1.5f)
        assertEquals(1.5f, r.angle, 1e-6f)
    }

    /**
     * Held on edge and turned face-down, a pitch near 180 is a small tilt, not a huge
     * one. Without normalising, a level held the other way up reads 178 degrees off.
     */
    @Test
    fun `a pitch near one eighty is a small angle`() {
        val r = Reading(Orientation.LEFT, pitch = 178f, roll = 0f, balance = 0f)
        assertEquals(-2f, r.angle, 1e-4f)
    }

    @Test
    fun `flat reports the worse of the two axes`() {
        val r = Reading(Orientation.FLAT, pitch = 1f, roll = -4f, balance = 0f)
        assertEquals(-4f, r.angle, 1e-4f)
    }
}
