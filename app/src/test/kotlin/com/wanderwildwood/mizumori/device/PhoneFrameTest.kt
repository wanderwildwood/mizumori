package com.wanderwildwood.mizumori.device

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneFrameTest {
    private val g = 9.8f

    private fun check(want: Triple<Float, Float, Float>, got: Triple<Float, Float, Float>) {
        assertEquals(want.first, got.first, 1e-4f)
        assertEquals(want.second, got.second, 1e-4f)
        assertEquals(want.third, got.third, 1e-4f)
    }

    @Test fun stockIsUnchanged() = check(Triple(1f, 2f, 3f), phoneFrame(1f, 2f, 3f, turned = false))

    // Upright on a Kompakt reads z = -g; in the usual frame that is gravity along +y.
    @Test fun turnedUpright() = check(Triple(0f, g, 0f), phoneFrame(0f, 0f, -g, turned = true))
    @Test fun turnedFlatFaceUp() = check(Triple(0f, 0f, g), phoneFrame(g, 0f, 0f, turned = true))
    @Test fun turnedOnItsLeftSide() = check(Triple(g, 0f, 0f), phoneFrame(0f, -g, 0f, turned = true))
}
