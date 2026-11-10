package com.omegathor3.heartsaver.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.omegathor3.heartsaver.signal.PpgEstimator
import com.omegathor3.heartsaver.signal.SignalQuality
import java.util.ArrayDeque
import kotlin.math.abs

/**
 * Samples the central camera region in memory and resamples it to a 30 Hz timeline.
 * Invalid optics, abrupt movement and loss of fingertip contact clear the active window.
 */
class PpgFrameAnalyzer(private val onUpdate: (PpgFrameState) -> Unit) : ImageAnalysis.Analyzer {
    private val samples = ArrayDeque<Double>()
    private val recentBpm = ArrayDeque<Int>()
    private val recentConfidence = ArrayDeque<Double>()
    private val sampleRateHz = 30.0
    private val samplePeriodNs = (1_000_000_000.0 / sampleRateHz).toLong()
    private val estimator = PpgEstimator(sampleRateHz = sampleRateHz)
    private val qualityGate = PpgFrameQuality()
    private var previousFrameNs = 0L
    private var previousRed = 0.0
    private var nextSampleNs = 0L
    private var samplesSinceEstimate = 0

    override fun analyze(image: ImageProxy) {
        try {
            val plane = image.planes.firstOrNull()
            if (plane == null) {
                clearSignal()
                onUpdate(PpgFrameState(status = "Camera frame has no pixel plane — retrying"))
                return
            }
            val buffer = plane.buffer
            val width = image.width
            val height = image.height
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            if (pixelStride < 4 || width < 8 || height < 8) {
                clearSignal()
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
            var clipped = 0
            var fingerPixels = 0
            var count = 0
            for (y in top until bottom step 8) {
                for (x in left until right step 8) {
                    val index = y * rowStride + x * pixelStride
                    if (index < 0 || index + 2 >= buffer.limit()) continue
                    val red = buffer.get(index).toInt() and 0xff
                    val green = buffer.get(index + 1).toInt() and 0xff
                    val blue = buffer.get(index + 2).toInt() and 0xff
                    redSum += red
                    greenSum += green
                    blueSum += blue
                    if (red >= 250 || green >= 250 || blue >= 250) clipped++
                    if (red >= PpgFrameQuality.MIN_RED &&
                        red > green * PpgFrameQuality.RED_GREEN_RATIO &&
                        red > blue * PpgFrameQuality.RED_BLUE_RATIO
                    ) fingerPixels++
                    count++
                }
            }
            if (count == 0) {
                clearSignal()
                onUpdate(PpgFrameState(status = "No camera pixels available"))
                return
            }

            val optics = FrameOptics(
                red = redSum.toDouble() / count,
                green = greenSum.toDouble() / count,
                blue = blueSum.toDouble() / count,
                clippedFraction = clipped.toDouble() / count,
                fingerPixelFraction = fingerPixels.toDouble() / count
            )
            val quality = qualityGate.inspect(optics)
            if (!quality.usable) {
                // Preserve PpgFrameQuality's movement-recovery counter. Calling
                // clearSignal() here would reset that counter after the first
                // movement frame and allow the very next frame back into sampling.
                resetSampling()
                onUpdate(PpgFrameState(status = quality.status))
                return
            }

            val timestamp = image.imageInfo.timestamp
            if (previousFrameNs == 0L || timestamp <= previousFrameNs ||
                timestamp - previousFrameNs > MAX_FRAME_GAP_NS
            ) {
                resetSampling()
                previousFrameNs = timestamp
                previousRed = optics.red
                nextSampleNs = timestamp + samplePeriodNs
                onUpdate(PpgFrameState(status = "Keep still — collecting pulse signal"))
                return
            }

            val intervalNs = timestamp - previousFrameNs
            while (nextSampleNs <= timestamp) {
                val fraction = ((nextSampleNs - previousFrameNs).toDouble() / intervalNs)
                    .coerceIn(0.0, 1.0)
                val interpolated = previousRed + (optics.red - previousRed) * fraction
                samples.addLast(interpolated)
                while (samples.size > MAX_SAMPLES) samples.removeFirst()
                samplesSinceEstimate++
                nextSampleNs += samplePeriodNs
            }
            previousFrameNs = timestamp
            previousRed = optics.red

            if (samples.size < REQUIRED_SAMPLES) {
                onUpdate(PpgFrameState(
                    status = "Keep still — collecting pulse signal",
                    progress = (samples.size.toFloat() / REQUIRED_SAMPLES).coerceIn(0f, 1f),
                    sampleCount = samples.size,
                    waveform = normalizedWaveform()
                ))
                return
            }

            if (samplesSinceEstimate >= ESTIMATE_INTERVAL_SAMPLES) {
                samplesSinceEstimate = 0
                val estimate = estimator.estimate(samples.toList())
                if (estimate != null && estimate.quality == SignalQuality.USABLE) {
                    recentBpm.addLast(estimate.bpm)
                    recentConfidence.addLast(estimate.confidence)
                    while (recentBpm.size > CONFIRMATION_WINDOWS) recentBpm.removeFirst()
                    while (recentConfidence.size > CONFIRMATION_WINDOWS) recentConfidence.removeFirst()
                    if (recentBpm.size >= MIN_CONFIRMATION_WINDOWS) {
                        val confirmedBpm = recentBpm.toList().sorted()[recentBpm.size / 2]
                        val confirmedConfidence = recentConfidence.toList().sorted()[recentConfidence.size / 2]
                        onUpdate(PpgFrameState(
                            status = "Confirmed across multiple windows — keep still",
                            bpm = confirmedBpm,
                            confidence = confirmedConfidence,
                            progress = 1f,
                            sampleCount = samples.size,
                            waveform = normalizedWaveform()
                        ))
                    } else {
                        onUpdate(PpgFrameState(
                            status = "Confirming pulse across another signal window…",
                            progress = 1f,
                            sampleCount = samples.size,
                            waveform = normalizedWaveform()
                        ))
                    }
                } else {
                    recentBpm.clear()
                    recentConfidence.clear()
                    onUpdate(PpgFrameState(
                        status = "Signal is weak or irregular — adjust gently and stay still",
                        progress = 1f,
                        sampleCount = samples.size,
                        waveform = normalizedWaveform()
                    ))
                }
            }
        } catch (_: IllegalStateException) {
            clearSignal()
            onUpdate(PpgFrameState(status = "Camera frame unavailable — retrying"))
        } finally {
            image.close()
        }
    }

    private fun normalizedWaveform(): List<Float> {
        val values = samples.toList().takeLast(WAVEFORM_SAMPLES)
        if (values.isEmpty()) return emptyList()
        val mean = values.average()
        val centered = values.map { it - mean }
        val scale = centered.maxOf { abs(it) }.coerceAtLeast(0.01)
        return centered.map { (it / scale).toFloat().coerceIn(-1f, 1f) }
    }

    private fun clearSignal() {
        resetSampling()
        qualityGate.reset()
    }

    private fun resetSampling() {
        samples.clear()
        recentBpm.clear()
        recentConfidence.clear()
        previousFrameNs = 0L
        previousRed = 0.0
        nextSampleNs = 0L
        samplesSinceEstimate = 0
    }

    companion object {
        private const val MAX_FRAME_GAP_NS = 120_000_000L
        private const val MAX_SAMPLES = 450
        private const val REQUIRED_SAMPLES = 240
        private const val ESTIMATE_INTERVAL_SAMPLES = 30
        private const val WAVEFORM_SAMPLES = 90
        private const val CONFIRMATION_WINDOWS = 5
        private const val MIN_CONFIRMATION_WINDOWS = 2
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
