package com.omegathor3.heartsaver.signal

import kotlin.math.sqrt

/**
 * Conservative pulse-rate estimator for a uniformly sampled fingertip PPG signal.
 *
 * This is an experimental signal-processing component, not a clinical algorithm.
 * Input values should represent mean red-channel intensity from each camera frame.
 */
data class PulseEstimate(
    val bpm: Int,
    val confidence: Double,
    val quality: SignalQuality
)

enum class SignalQuality { INSUFFICIENT_DATA, TOO_NOISY, USABLE }

class PpgEstimator(
    private val sampleRateHz: Double = 30.0,
    private val minBpm: Int = 40,
    private val maxBpm: Int = 180
) {
    fun estimate(samples: List<Double>): PulseEstimate? {
        if (sampleRateHz <= 0.0 || samples.size < (sampleRateHz * 8.0).toInt()) return null
        if (samples.any { !it.isFinite() }) return null

        val mean = samples.average()
        val centered = samples.map { it - mean }
        val rms = sqrt(centered.sumOf { it * it } / centered.size)
        if (rms < 0.8) return null

        // Remove slow illumination drift with a centered moving-average baseline.
        val radius = (sampleRateHz * 0.75).toInt().coerceAtLeast(2)
        val detrended = DoubleArray(centered.size) { i ->
            val from = (i - radius).coerceAtLeast(0)
            val to = (i + radius).coerceAtMost(centered.lastIndex)
            val baseline = centered.subList(from, to + 1).average()
            centered[i] - baseline
        }

        val energy = sqrt(detrended.sumOf { it * it } / detrended.size)
        if (energy < 0.25) return null

        val minLag = (sampleRateHz * 60.0 / maxBpm).toInt().coerceAtLeast(1)
        val maxLag = (sampleRateHz * 60.0 / minBpm).toInt().coerceAtMost(detrended.size / 3)
        if (maxLag <= minLag) return null

        var bestLag = -1
        var bestCorrelation = Double.NEGATIVE_INFINITY
        for (lag in minLag..maxLag) {
            var cross = 0.0
            var leftEnergy = 0.0
            var rightEnergy = 0.0
            for (i in 0 until detrended.size - lag) {
                val a = detrended[i]
                val b = detrended[i + lag]
                cross += a * b
                leftEnergy += a * a
                rightEnergy += b * b
            }
            val denominator = sqrt(leftEnergy * rightEnergy)
            val correlation = if (denominator > 0.0) cross / denominator else 0.0
            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestLag = lag
            }
        }

        if (bestLag <= 0 || bestCorrelation < 0.35) return null
        val bpm = (60.0 * sampleRateHz / bestLag).toInt()
        if (bpm !in minBpm..maxBpm) return null

        return PulseEstimate(
            bpm = bpm,
            confidence = bestCorrelation.coerceIn(0.0, 1.0),
            quality = if (bestCorrelation >= 0.55) SignalQuality.USABLE else SignalQuality.TOO_NOISY
        )
    }
}
