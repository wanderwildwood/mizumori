package com.wanderwildwood.mizumori.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GravityTest {

    @Test
    fun `lying flat reads level`() {
        val r = Gravity.reading(0f, 0f, 9.81f)!!
        assertEquals(Orientation.FLAT, r.orientation)
        assertEquals(0f, r.roll, 1e-3f)
        assertEquals(0f, r.pitch, 1e-3f)
    }

    @Test
    fun `free fall has no reading`() {
        assertNull(Gravity.reading(0f, 0f, 0f))
    }

    @Test
    fun `high on the right edge gives a positive across angle`() {
        val r = Gravity.reading(1.0f, 0f, 9.76f)!!
        assertEquals(5.85f, r.acrossAngle, 0.05f)
        assertEquals(0f, r.alongAngle, 1e-3f)
    }

    @Test
    fun `high at the top gives a negative along angle`() {
        val r = Gravity.reading(0f, 1.0f, 9.76f)!!
        assertEquals(-5.85f, r.alongAngle, 0.05f)
    }

    @Test
    fun `standing on the bottom edge is the top orientation and balance is the lean sideways`() {
        val r = Gravity.reading(0.5f, 9.8f, 0f)!!
        assertEquals(Orientation.TOP, r.orientation)
        assertEquals(2.9f, r.balance, 0.1f)
    }

    @Test
    fun `on its long edge the phone is the left or right orientation`() {
        assertEquals(Orientation.RIGHT, Gravity.reading(9.8f, 0f, 0f)!!.orientation)
        assertEquals(Orientation.LEFT, Gravity.reading(-9.8f, 0f, 0f)!!.orientation)
    }
}

class ScreenFrameTest {

    /** Phone's x axis high by about 5.7 degrees, lying flat. */
    private val xHigh = Gravity.reading(0.97f, 0f, 9.75f)!!

    /** Phone's y axis (the top of the screen when upright) high by about 5.7 degrees. */
    private val yHigh = Gravity.reading(0f, 0.97f, 9.75f)!!

    private fun flat(r: Reading, rotation: Int) = ScreenFrame.flat(r, rotation)

    @Test
    fun `upright, the bubble goes to the high side`() {
        val (right, down) = flat(xHigh, 0)
        assertTrue(right > 5f); assertEquals(0f, down, 1e-3f)
        val (r2, d2) = flat(yHigh, 0)
        assertEquals(0f, r2, 1e-3f); assertTrue(d2 < -5f)
    }

    /**
     * Turned a quarter, the phone's own y axis runs along the screen's width (to the right
     * at rotation 1), so a high y axis is a bubble to the right.
     */
    @Test
    fun `turned a quarter, a high y axis is to the right`() {
        val (right, down) = flat(yHigh, 1)
        assertTrue(right > 5f); assertEquals(0f, down, 1e-3f)
        val (r2, d2) = flat(xHigh, 1)
        assertEquals(0f, r2, 1e-3f); assertTrue(d2 > 5f)
    }

    @Test
    fun `turned three quarters, the other way round`() {
        val (right, _) = flat(yHigh, 3)
        assertTrue(right < -5f)
        val (_, down) = flat(xHigh, 3)
        assertTrue(down < -5f)
    }

    @Test
    fun `upside down both axes flip`() {
        val (right, down) = flat(xHigh, 2)
        assertTrue(right < -5f); assertEquals(0f, down, 1e-3f)
    }

    @Test
    fun `the same physical tilt has the same size on every rotation`() {
        for (rotation in 0..3) {
            val (right, down) = flat(xHigh, rotation)
            assertEquals(xHigh.acrossAngle, Math.hypot(right.toDouble(), down.toDouble()).toFloat(), 1e-3f)
        }
    }

    @Test
    fun `four quarter turns in a row come back to the start`() {
        val (r0, d0) = flat(xHigh, 0)
        val (r4, d4) = flat(xHigh, 4)
        assertEquals(r0, r4, 0f); assertEquals(d0, d4, 0f)
    }

    @Test
    fun `calibration offsets are subtracted in the phone frame so rotation cannot move them`() {
        // A phone that reads 1 degree high on its x axis on a true level: calibrate, then
        // read it at every rotation and the bubble must sit in the middle.
        val raw = Gravity.reading(0.171f, 0f, 9.808f)!!
        val zero = raw.roll
        val corrected = raw.copy(roll = raw.roll - zero)
        for (rotation in 0..3) {
            val (right, down) = ScreenFrame.flat(corrected, rotation)
            assertEquals(0f, right, 1e-3f); assertEquals(0f, down, 1e-3f)
        }
    }

    @Test
    fun `on its long edge turned sideways, the high end is the bubble's side`() {
        // Standing on the long edge, screen turned a quarter: the phone's +y end is to the right.
        val yEndHigh = Gravity.reading(9.8f, 0.5f, 0f)!!  // on edge, +y end high
        assertEquals(Orientation.RIGHT, yEndHigh.orientation)
        val quarter = ScreenFrame.edge(yEndHigh, 1)
        val threeQuarters = ScreenFrame.edge(yEndHigh, 3)
        assertTrue(quarter > 0f)
        assertEquals(-quarter, threeQuarters, 1e-3f)
    }

    @Test
    fun `upright on its short edge reads as it always did, upside down is reversed`() {
        val r = Gravity.reading(0.5f, 9.8f, 0f)!!
        assertEquals(r.balance, ScreenFrame.edge(r, 0), 0f)
        assertEquals(-r.balance, ScreenFrame.edge(r, 2), 0f)
    }

    @Test
    fun `level does not depend on the rotation`() {
        assertNotNull(xHigh)
        assertTrue(xHigh.orientation.isLevel(0.05f, 0.05f, 0f, 0.2f))
    }

    @Test
    fun `landscape is the odd quarter turns`() {
        assertTrue(!ScreenFrame.isLandscape(0)); assertTrue(ScreenFrame.isLandscape(1))
        assertTrue(!ScreenFrame.isLandscape(2)); assertTrue(ScreenFrame.isLandscape(3))
    }
}
