package com.omegathor3.heartsaver.signal

import kotlin.math.abs
import kotlin.math.sqrt

data class PulseEstimate(val bpm: Int, val confidence: Double, val quality: SignalQuality)
enum class SignalQuality { INSUFFICIENT_DATA, TOO_NOISY, USABLE }

/** Experimental camera-PPG estimator; not clinically validated. */
class PpgEstimator(
    private val sampleRateHz: Double = 30.0,
    private val minBpm: Int = 40,
    private val maxBpm: Int = 180
) {
    fun estimate(samples: List<Double>): PulseEstimate? {
        if (!sampleRateHz.isFinite() || sampleRateHz !in 10.0..120.0 ||
            minBpm < 30 || maxBpm > 220 || minBpm >= maxBpm ||
            samples.size < (sampleRateHz * 8.0).toInt()
        ) return null
        if (samples.any { !it.isFinite() }) return null

        val mean = samples.average()
        if (!mean.isFinite() || mean <= 1.0) return null

        val centered = samples.map { it - mean }
        val rawRms = sqrt(centered.sumOf { it * it } / centered.size)
        if (rawRms / mean < 0.002) return null

        val radius = (sampleRateHz * 0.75).toInt().coerceAtLeast(2)
        val detrended = DoubleArray(centered.size) { index ->
            val from = (index - radius).coerceAtLeast(0)
            val to = (index + radius).coerceAtMost(centered.lastIndex)
            centered[index] - centered.subList(from, to + 1).average()
        }
        val relativeEnergy = sqrt(detrended.sumOf { it * it } / detrended.size) / mean
        if (!relativeEnergy.isFinite() || relativeEnergy < 0.001) return null

        val minLag = (sampleRateHz * 60.0 / maxBpm).toInt().coerceAtLeast(1)
        val maxLag = (sampleRateHz * 60.0 / minBpm).toInt().coerceAtMost(detrended.size / 3)
        if (maxLag <= minLag) return null

        val correlations = DoubleArray(maxLag + 1) { Double.NEGATIVE_INFINITY }
        var bestLag = -1
        var bestCorrelation = Double.NEGATIVE_INFINITY

        // Keep one lag before the supported range for interpolation at minLag.
        for (lag in (minLag - 1).coerceAtLeast(1)..maxLag) {
            var cross = 0.0
            var left = 0.0
            var right = 0.0
            for (index in 0 until detrended.size - lag) {
                val a = detrended[index]
                val b = detrended[index + lag]
                cross += a * b
                left += a * a
                right += b * b
            }
            val denominator = sqrt(left * right)
            val correlation = if (denominator > 0.0) cross / denominator else 0.0
            correlations[lag] = correlation
            if (lag >= minLag && correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestLag = lag
            }
        }

        if (bestLag <= 0 || bestCorrelation < 0.35) return null

        // Autocorrelation can be slightly stronger at 2x the real period. Prefer
        // the first reliable local peak so a strong harmonic does not halve BPM.
        val firstReliablePeak = (minLag..maxLag).firstOrNull { lag ->
            val value = correlations[lag]
            val previous = correlations[lag - 1]
            val next = if (lag < maxLag) correlations[lag + 1] else Double.NEGATIVE_INFINITY
            value >= 0.55 && value >= previous && value >= next
        }
        val selectedLag = firstReliablePeak ?: bestLag
        val selectedCorrelation = correlations[selectedLag]

        val offset = if (selectedLag > 1 && selectedLag < maxLag) {
            val left = correlations[selectedLag - 1]
            val center = correlations[selectedLag]
            val right = correlations[selectedLag + 1]
            val curve = left - 2.0 * center + right
            if (left.isFinite() && right.isFinite() && abs(curve) > 1e-9) {
                (0.5 * (left - right) / curve).coerceIn(-0.5, 0.5)
            } else {
                0.0
            }
        } else {
            0.0
        }

        val bpm = (60.0 * sampleRateHz / (selectedLag + offset)).toInt()
        if (bpm !in minBpm..maxBpm) return null

        return PulseEstimate(
            bpm,
            selectedCorrelation.coerceIn(0.0, 1.0),
            if (selectedCorrelation >= 0.55) SignalQuality.USABLE else SignalQuality.TOO_NOISY
        )
    }
}
