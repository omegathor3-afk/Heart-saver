package com.omegathor3.heartsaver.signal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PpgEstimatorTest {
    private val estimator = PpgEstimator(sampleRateHz = 30.0)

    @Test fun estimatesSyntheticSeventyTwoBpmSignal() {
        val samples = (0 until 600).map { i ->
            120.0 + 8.0 * sin(2.0 * PI * 1.2 * i / 30.0)
        }
        val result = estimator.estimate(samples)
        assertTrue(result != null)
        assertTrue(kotlin.math.abs(result!!.bpm - 72) <= 2)
    }

    @Test fun rejectsSignalThatIsTooShort() {
        assertNull(estimator.estimate(List(100) { 100.0 }))
    }

    @Test fun rejectsFlatSignalInsteadOfInventingAReading() {
        assertNull(estimator.estimate(List(600) { 100.0 }))
    }

    @Test fun rejectsNonFiniteInput() {
        val samples = MutableList(600) { 100.0 }
        samples[200] = Double.NaN
        assertNull(estimator.estimate(samples))
    }
}
