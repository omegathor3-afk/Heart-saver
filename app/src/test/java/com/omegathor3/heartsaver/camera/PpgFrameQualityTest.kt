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

    @Test
    fun validatesPlaneStartingAtNonZeroBufferPosition() {
        assertTrue(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 256, pixelStride = 4,
            bufferLimit = 12304, bufferPosition = 16
        ).isValid())
        assertFalse(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 256, pixelStride = 4,
            bufferLimit = 12303, bufferPosition = 16
        ).isValid())
        assertFalse(PpgRgbaPlaneLayout(
            width = 64, height = 48, rowStride = 256, pixelStride = 4,
            bufferLimit = 12304, bufferPosition = 12305
        ).isValid())
    }

    @Test
    fun acceptsPaddedRowsOnlyWhenTheCompleteLastRowFits() {
        val width = 64
        val height = 48
        val rowStride = 272 // 16 bytes of row padding beyond RGBA pixels.
        val pixelBytes = width * 4
        val position = 16
        val requiredLimit = position + (height - 1) * rowStride + pixelBytes

        assertTrue(PpgRgbaPlaneLayout(
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = 4,
            bufferLimit = requiredLimit,
            bufferPosition = position
        ).isValid())

        assertFalse(PpgRgbaPlaneLayout(
            width = width,
            height = height,
            rowStride = rowStride,
            pixelStride = 4,
            bufferLimit = requiredLimit - 1,
            bufferPosition = position
        ).isValid())
    }


    @Test
    fun resamplesJitteredCameraFramesOnAFixedThirtyHertzTimeline() {
        val resampler = PpgTimestampResampler(sampleRateHz = 30.0)
        assertTrue(resampler.push(0L, 100.0).reset)
        assertTrue(resampler.push(20_000_000L, 106.0).samples.isEmpty())

        val result = resampler.push(75_000_000L, 117.0)
        assertFalse(result.reset)
        assertEquals(2, result.samples.size)
        assertEquals(108.6666666667, result.samples[0], 0.000001)
        assertEquals(115.3333333333, result.samples[1], 0.000001)
    }

    @Test
    fun longGapsAndNonMonotonicTimestampsStartFreshWindows() {
        val resampler = PpgTimestampResampler(sampleRateHz = 30.0)
        assertTrue(resampler.push(0L, 100.0).reset)
        assertTrue(resampler.push(121_000_000L, 110.0).reset)

        val afterGap = resampler.push(154_333_333L, 120.0)
        assertFalse(afterGap.reset)
        assertEquals(listOf(120.0), afterGap.samples)

        assertTrue(resampler.push(150_000_000L, 130.0).reset)
        val afterClockReset = resampler.push(183_333_333L, 140.0)
        assertFalse(afterClockReset.reset)
        assertEquals(listOf(140.0), afterClockReset.samples)
    }

    @Test
    fun invalidSamplesResetTheTimelineAndZeroIsAValidFirstTimestamp() {
        val resampler = PpgTimestampResampler(sampleRateHz = 30.0)
        assertTrue(resampler.push(0L, 100.0).reset)
        assertTrue(resampler.push(33_333_333L, Double.NaN).reset)
        assertTrue(resampler.push(66_666_666L, 110.0).reset)
        val next = resampler.push(99_999_999L, 120.0)
        assertFalse(next.reset)
        assertEquals(listOf(120.0), next.samples)
    }


}
