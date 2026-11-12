package com.omegathor3.heartsaver.signal

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class PpgEstimatorTest {
    private val estimator = PpgEstimator(sampleRateHz = 30.0)

    private fun pulse(
        bpm: Double,
        offset: Double = 120.0,
        amplitude: Double = 8.0,
        drift: Double = 0.0
    ) = (0 until 600).map { i ->
        offset + amplitude * sin(2.0 * PI * bpm / 60.0 * i / 30.0) + drift * i / 600.0
    }

    @Test fun estimatesSyntheticSeventyTwoBpmSignal() {
        val result = estimator.estimate(pulse(72.0))
        assertTrue(result != null)
        assertTrue(abs(result!!.bpm - 72) <= 2)
    }

    @Test fun estimatesPulseWithDeterministicNoiseAndFractionalPeriod() {
        val random = Random(42)
        val samples = (0 until 600).map {
            110.0 + 7.0 * sin(2.0 * PI * 1.23 * it / 30.0) + random.nextDouble(-1.2, 1.2)
        }
        val result = estimator.estimate(samples)
        assertTrue(result != null)
        assertTrue(abs(result!!.bpm - 74) <= 2)
    }

    @Test fun calibrationIsRelativelyInvariantToExposureScale() {
        val dim = estimator.estimate(pulse(84.0, 70.0, 0.7))
        val bright = estimator.estimate(pulse(84.0, 180.0, 1.8))
        assertTrue(dim != null)
        assertTrue(bright != null)
        assertTrue(abs(dim!!.bpm - bright!!.bpm) <= 1)
    }

    @Test fun removesSlowLinearIlluminationDrift() {
        val result = estimator.estimate(pulse(66.0, drift = 24.0))
        assertTrue(result != null)
        assertTrue(abs(result!!.bpm - 66) <= 2)
    }

    @Test fun prefersTheFirstReliablePeakOverA_StrongerHalfRateHarmonic() {
        listOf(84.0, 170.0, 175.0, 180.0).forEach { expected ->
            val result = estimator.estimate(pulse(expected))
            assertTrue("No estimate for synthetic $expected BPM signal", result != null)
            assertTrue(
                "Expected approximately $expected BPM, received ${result!!.bpm}",
                abs(result!!.bpm - expected.toInt()) <= 2
            )
        }
    }

    @Test fun rejectsShortFlatAndNonFiniteSignals() {
        assertNull(estimator.estimate(List(100) { 100.0 }))
        assertNull(estimator.estimate(List(600) { 100.0 }))
        assertNull(estimator.estimate(List(239) { 100.0 + sin(it.toDouble()) }))
        val nan = MutableList(600) { 100.0 }
        nan[200] = Double.NaN
        assertNull(estimator.estimate(nan))
        val infinity = MutableList(600) { 100.0 }
        infinity[200] = Double.POSITIVE_INFINITY
        assertNull(estimator.estimate(infinity))
    }

    @Test fun rejectsInvalidConfiguration() {
        assertNull(PpgEstimator(sampleRateHz = Double.NaN).estimate(pulse(72.0)))
        assertNull(PpgEstimator(sampleRateHz = 500.0).estimate(pulse(72.0)))
        assertNull(PpgEstimator(minBpm = 180, maxBpm = 180).estimate(pulse(72.0)))
    }

    @Test fun estimateReportsBoundedConfidenceAndSupportedBpm() {
        val result = estimator.estimate(pulse(96.0))!!
        assertTrue(result.bpm in 40..180)
        assertTrue(result.confidence in 0.0..1.0)
    }
}
