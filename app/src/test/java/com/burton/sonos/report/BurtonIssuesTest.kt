package com.burton.sonos.report

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BurtonIssuesTest {
    @Test
    fun catalogPackageStripsDebugSuffix() {
        assertEquals("com.burton.sonos", BurtonIssues.catalogPackage("com.burton.sonos.debug"))
        assertEquals("com.burton.sonos", BurtonIssues.catalogPackage("com.burton.sonos"))
        assertEquals("com.burton.chat", BurtonIssues.catalogPackage("com.burton.chat.debug"))
    }

    @Test
    fun newIssueUriUsesCatalogPackage() {
        assertEquals(
            "burtonissues://new?package=com.burton.sonos",
            BurtonIssues.newIssueUri("com.burton.sonos.debug"),
        )
        assertEquals(
            "burtonissues://new?package=com.burton.sonos",
            BurtonIssues.newIssueUri("com.burton.sonos"),
        )
    }
}

class ShakeDetectorTest {
    @Test
    fun restAndSingleBumpDoNotFire() {
        var now = 0L
        var fired = 0
        val detector = ShakeDetector(nowMs = { now }, onShake = { fired += 1 })
        assertFalse(detector.onAcceleration(0f, 0f, 9.8f))
        now = 10
        assertFalse(peak(detector))
        assertEquals(0, fired)
    }

    @Test
    fun twoPeaksWithinWindowFireOnce() {
        var now = 0L
        var fired = 0
        val detector = ShakeDetector(nowMs = { now }, onShake = { fired += 1 })
        assertFalse(peak(detector))
        now = 200
        assertTrue(peak(detector))
        assertEquals(1, fired)
        now = 400
        assertFalse(peak(detector))
        assertEquals(1, fired)
    }

    @Test
    fun peaksOutsideWindowDoNotCount() {
        var now = 0L
        var fired = 0
        val detector = ShakeDetector(nowMs = { now }, onShake = { fired += 1 })
        assertFalse(peak(detector))
        now = 1_600
        assertFalse(peak(detector))
        assertEquals(0, fired)
    }

    @Test
    fun cooldownBlocksImmediateRefire() {
        var now = 0L
        var fired = 0
        val detector = ShakeDetector(nowMs = { now }, onShake = { fired += 1 })
        peak(detector)
        now = 200
        assertTrue(peak(detector))
        now = 400
        peak(detector)
        now = 600
        assertFalse(peak(detector))
        assertEquals(1, fired)
        now = 2_300
        peak(detector)
        now = 2_500
        assertTrue(peak(detector))
        assertEquals(2, fired)
    }

    private fun peak(detector: ShakeDetector): Boolean =
        detector.onAcceleration(20f, 20f, 20f)
}
