package com.omegathor3.heartsaver.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PpgFrameQualityTest {
    private fun stable(
        red: Double = 150.0,
        green: Double = 95.0,
        blue: Double = 80.0,
        clipped: Double = 0.02,
        coverage: Double = 0.9
    ) = FrameOptics(red, green, blue, clipped, coverage)

    @Test
    fun acceptsStableFingerLikeOptics() {
        val gate = PpgFrameQuality()
        assertTrue(gate.inspect(stable()).usable)
        assertTrue(gate.inspect(stable(151.0, 95.5, 80.5)).usable)
    }

    @Test
    fun rejectsUncoveredPartialAndInvalidCoverage() {
        val gate = PpgFrameQuality()
        assertFalse(gate.inspect(stable(90.0, 100.0, 95.0)).fingerDetected)
        assertFalse(gate.inspect(stable(coverage = 0.30)).usable)
        assertFalse(gate.inspect(stable(coverage = Double.NaN)).usable)
        assertFalse(gate.inspect(stable(coverage = 1.2)).usable)
    }

    @Test
    fun rejectsDarkBrightAndClippedFrames() {
        val gate = PpgFrameQuality()
        assertFalse(gate.inspect(stable(20.0, 15.0, 12.0)).usable)
        assertFalse(gate.inspect(stable(250.0, 245.0, 240.0)).usable)
        assertFalse(gate.inspect(stable(clipped = 0.45)).usable)
        assertFalse(gate.inspect(stable(clipped = -0.1)).usable)
    }

    @Test
    fun rejectsRgbChannelsOutsideCameraByteRange() {
        val gate = PpgFrameQuality()
        assertFalse(gate.inspect(stable(red = -1.0)).usable)
        assertFalse(gate.inspect(stable(green = 256.0)).usable)
        assertFalse(gate.inspect(stable(blue = Double.POSITIVE_INFINITY)).usable)
        assertFalse(gate.inspect(stable(red = Double.NaN)).usable)
    }

    @Test
    fun movementRequiresRecoveryFrames() {
        val gate = PpgFrameQuality()
        assertTrue(gate.inspect(stable()).usable)
        val moved = gate.inspect(stable(205.0, 130.0, 110.0))
        assertFalse(moved.usable)
        assertTrue(moved.status.contains("Movement"))
        assertFalse(gate.inspect(stable(205.5, 130.2, 110.1)).usable)
        assertFalse(gate.inspect(stable(205.2, 130.1, 110.0)).usable)
        assertTrue(gate.inspect(stable(205.1, 130.0, 110.0)).usable)
    }

    @Test
    fun rejectsNonFiniteOptics() {
        assertFalse(PpgFrameQuality().inspect(stable(Double.NaN)).usable)
    }

    @Test
    fun invalidOpticsResetBaseline() {
        val gate = PpgFrameQuality()
        gate.inspect(stable())
        assertFalse(gate.inspect(stable(20.0, 15.0, 12.0)).usable)
        assertTrue(gate.inspect(stable(205.0, 130.0, 110.0)).usable)
    }
    @Test
    fun validatesCompleteRgbaPlaneBeforeSampling() {
        assertTrue(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 256, pixelStride = 4, bufferLimit = 12288
        ).isValid())

        assertFalse(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 255, pixelStride = 4, bufferLimit = 12288
        ).isValid())
        assertFalse(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 256, pixelStride = 4, bufferLimit = 12287
        ).isValid())
        assertFalse(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 512, pixelStride = 8, bufferLimit = 24576
        ).isValid())
        assertFalse(PpgRgbaPlaneLayout(
            width = 7, height = 48, rowStride = 28, pixelStride = 4, bufferLimit = 1344
        ).isValid())
        assertFalse(PpgRgbaPlaneLayout(
            width = Int.MAX_VALUE, height = Int.MAX_VALUE,
            rowStride = Int.MAX_VALUE, pixelStride = 4, bufferLimit = Int.MAX_VALUE
        ).isValid())
    }

}
