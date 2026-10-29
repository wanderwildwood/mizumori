package com.wanderwildwood.mizumori.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TapAgainTest {
    @Test fun `the first tap only asks`() {
        val r = TapAgain.press(false)
        assertTrue(r.armed)
        assertFalse(r.confirmed)
    }

    @Test fun `the second tap confirms and disarms`() {
        val r = TapAgain.press(true)
        assertFalse(r.armed)
        assertTrue(r.confirmed)
    }

    @Test fun `asking again after a confirm starts over`() {
        assertEquals(TapAgain.Result(true, false), TapAgain.press(TapAgain.press(true).armed))
    }
}
