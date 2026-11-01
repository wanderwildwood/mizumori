package com.wanderwildwood.mizumori.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Readings are raw accelerometer values in the usual Android frame, as logged on the Kompakt (31 Oct). */
class RecordedPoseTest {
    @Test fun lyingFaceUpOnATableIsFlat() =
        assertEquals(Orientation.FLAT, Gravity.reading(0.16f, 0.0f, 9.58f)?.orientation)

    @Test fun slightlyTiltedFaceUpIsStillFlat() =
        assertEquals(Orientation.FLAT, Gravity.reading(1.5f, 1.0f, 9.5f)?.orientation)

    @Test fun heldUprightLeanedBackStandsOnItsBottomEdge() =
        assertEquals(Orientation.TOP, Gravity.reading(0.2f, 9.4f, 2.8f)?.orientation)

    @Test fun heldSidewaysIsOnAnEdge() {
        assertEquals(Orientation.RIGHT, Gravity.reading(9.8f, -0.1f, 1.3f)?.orientation)
        assertEquals(Orientation.LEFT, Gravity.reading(-9.5f, -0.2f, 1.7f)?.orientation)
    }

    @Test fun lyingFaceDownIsNotTreatedAsFlat() =
        assertNotEquals(Orientation.FLAT, Gravity.reading(0.1f, 0.1f, -9.8f)?.orientation)
}
