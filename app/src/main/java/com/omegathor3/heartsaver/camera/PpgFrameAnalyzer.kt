package com.omegathor3.heartsaver.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.omegathor3.heartsaver.signal.PpgEstimator
import java.util.ArrayDeque

/**
 * Extracts central-region red-channel samples and resamples them onto a fixed 30 Hz clock.
 * Camera frames and samples are kept in memory only; no image data is saved.
 */
class PpgFrameAnalyzer(private val onUpdate: (PpgFrameState) -> Unit) : ImageAnalysis.Analyzer {
    private val samples = ArrayDeque<Double>()
    private val sampleRateHz = 30.0
    private val samplePeriodNs = (1_000_000_000.0 / sampleRateHz).toLong()
    private val estimator = PpgEstimator(sampleRateHz = sampleRateHz)
    private var previousFrameNs = 0L
    private var previousRed = 0.0
    private var nextSampleNs = 0L
    private var samplesSinceEstimate = 0
    private var lastFingerPresent = false

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes.firstOrNull() ?: return
            val buffer = plane.buffer
            val width = image.width
            val height = image.height
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            if (pixelStride < 4 || width < 8 || height < 8) {
                onUpdate(PpgFrameState(status = "Camera format not supported"))
                return
            }

            val left = width / 4
            val right = width * 3 / 4
            val top = height / 4
            val bottom = height * 3 / 4
            var redSum = 0L
            var greenSum = 0L
            var blueSum = 0L
            var count = 0
            for (y in top until bottom step 8) {
                for (x in left until right step 8) {
                    val index = y * rowStride + x * pixelStride
                    if (index + 2 >= buffer.limit()) continue
                    redSum += buffer.get(index).toInt() and 0xff
                    greenSum += buffer.get(index + 1).toInt() and 0xff
                    blueSum += buffer.get(index + 2).toInt() and 0xff
                    count++
                }
            }
            if (count == 0) return

            val red = redSum.toDouble() / count
            val green = greenSum.toDouble() / count
            val blue = blueSum.toDouble() / count
            val fingerPresent = red > 65.0 && red > green * 1.06 && red > blue * 1.06
            if (!fingerPresent) {
                if (lastFingerPresent) reset()
                lastFingerPresent = false
                onUpdate(PpgFrameState(status = "Cover the rear camera and flash with your fingertip"))
                return
            }

            val timestamp = image.imageInfo.timestamp
            if (previousFrameNs == 0L || timestamp <= previousFrameNs ||
                timestamp - previousFrameNs > 120_000_000L
            ) {
                reset()
                previousFrameNs = timestamp
                previousRed = red
                nextSampleNs = timestamp + samplePeriodNs
                lastFingerPresent = true
                onUpdate(PpgFrameState(status = "Keep still — collecting pulse signal"))
                return
            }

            // Interpolate camera readings at exact 30 Hz timestamps so the estimator's
            // sample-rate assumption does not depend on the phone's variable frame rate.
            val intervalNs = timestamp - previousFrameNs
            while (nextSampleNs <= timestamp) {
                val fraction = ((nextSampleNs - previousFrameNs).toDouble() / intervalNs)
                    .coerceIn(0.0, 1.0)
                val interpolated = previousRed + (red - previousRed) * fraction
                samples.addLast(interpolated)
                while (samples.size > 450) samples.removeFirst()
                samplesSinceEstimate++
                nextSampleNs += samplePeriodNs
            }
            previousFrameNs = timestamp
            previousRed = red
            lastFingerPresent = true

            if (samples.size < 240) {
                onUpdate(PpgFrameState(
                    status = "Keep still — collecting pulse signal",
                    progress = (samples.size.toFloat() / 240).coerceIn(0f, 1f),
                    sampleCount = samples.size,
                    waveform = normalizedWaveform()
                ))
                return
            }

            if (samplesSinceEstimate >= 60) {
                samplesSinceEstimate = 0
                val estimate = estimator.estimate(samples.toList())
                if (estimate != null && estimate.quality == com.omegathor3.heartsaver.signal.SignalQuality.USABLE) {
                    onUpdate(PpgFrameState(
                        status = "Signal detected — hold still",
                        bpm = estimate.bpm,
                        confidence = estimate.confidence,
                        progress = 1f,
                        sampleCount = samples.size,
                        waveform = normalizedWaveform()
                    ))
                } else {
                    onUpdate(PpgFrameState(
                        status = "Signal is unstable — adjust fingertip pressure and stay still",
                        progress = 1f,
                        sampleCount = samples.size,
                        waveform = normalizedWaveform()
                    ))
                }
            }
        } catch (_: IllegalStateException) {
            onUpdate(PpgFrameState(status = "Camera frame unavailable — retrying"))
        } finally {
            image.close()
        }
    }

    private fun normalizedWaveform(): List<Float> {
        val values = samples.toList().takeLast(90)
        if (values.isEmpty()) return emptyList()
        val mean = values.average()
        val centered = values.map { it - mean }
        val scale = centered.maxOf { kotlin.math.abs(it) }.coerceAtLeast(0.01)
        return centered.map { (it / scale).toFloat().coerceIn(-1f, 1f) }
    }

    private fun reset() {
        samples.clear()
        previousFrameNs = 0L
        previousRed = 0.0
        nextSampleNs = 0L
        samplesSinceEstimate = 0
    }
}

data class PpgFrameState(
    val status: String = "Ready",
    val bpm: Int? = null,
    val confidence: Double = 0.0,
    val progress: Float = 0f,
    val sampleCount: Int = 0,
    val waveform: List<Float> = emptyList()
)
