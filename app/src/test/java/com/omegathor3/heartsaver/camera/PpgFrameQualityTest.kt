package com.omegathor3.heartsaver.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PpgFrameQualityTest {
    @Test fun acceptsStableFingerLikeOptics() {
        val gate = PpgFrameQuality()
        val first = gate.inspect(FrameOptics(150.0, 95.0, 80.0, 0.02))
        val next = gate.inspect(FrameOptics(151.0, 95.5, 80.5, 0.02))
        assertTrue(first.usable)
        assertTrue(next.usable)
        assertTrue(next.fingerDetected)
    }

    @Test fun rejectsUncoveredCamera() {
        val result = PpgFrameQuality().inspect(FrameOptics(90.0, 100.0, 95.0, 0.0))
        assertFalse(result.usable)
        assertFalse(result.fingerDetected)
    }

    @Test fun rejectsPartialFingertipCoverage() {
        val result = PpgFrameQuality().inspect(FrameOptics(150.0, 95.0, 80.0, 0.01, 0.30))
        assertFalse(result.usable)
        assertFalse(result.fingerDetected)
    }

    @Test fun rejectsDarkAndOverexposedFrames() {
        val gate = PpgFrameQuality()
        assertFalse(gate.inspect(FrameOptics(20.0, 15.0, 12.0, 0.0)).usable)
        assertFalse(gate.inspect(FrameOptics(250.0, 245.0, 240.0, 0.0)).usable)
        assertFalse(gate.inspect(FrameOptics(160.0, 100.0, 80.0, 0.45)).usable)
    }

    @Test fun rejectsAbruptMovementOrPressureChange() {
        val gate = PpgFrameQuality()
        assertTrue(gate.inspect(FrameOptics(150.0, 95.0, 80.0, 0.01)).usable)
        val moved = gate.inspect(FrameOptics(205.0, 130.0, 110.0, 0.01))
        assertFalse(moved.usable)
        assertTrue(moved.status.contains("Movement"))
    }

    @Test fun rejectsNonFiniteOpticalValues() {
        val result = PpgFrameQuality().inspect(FrameOptics(Double.NaN, 90.0, 70.0, 0.0))
        assertFalse(result.usable)
        assertFalse(result.fingerDetected)
    }

    @Test fun resetsBaselineAfterInvalidFrame() {
        val gate = PpgFrameQuality()
        gate.inspect(FrameOptics(150.0, 95.0, 80.0, 0.01))
        gate.inspect(FrameOptics(20.0, 15.0, 12.0, 0.0))
        assertTrue(gate.inspect(FrameOptics(205.0, 130.0, 110.0, 0.01)).usable)
    }
}
