package com.omegathor3.heartsaver.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.omegathor3.heartsaver.signal.PpgEstimator
import java.util.ArrayDeque

/** Extracts central-region red-channel samples from CameraX RGBA frames. Frames are never saved. */
class PpgFrameAnalyzer(private val onUpdate: (PpgFrameState) -> Unit) : ImageAnalysis.Analyzer {
    private val samples = ArrayDeque<Double>()
    private val estimator = PpgEstimator(sampleRateHz = 30.0)
    private var lastSampleNs = 0L
    private var framesSinceEstimate = 0
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
            if (lastSampleNs != 0L) {
                val elapsedMs = (timestamp - lastSampleNs) / 1_000_000L
                if (elapsedMs > 120L) reset()
                if (elapsedMs < 28L) return
            }
            lastSampleNs = timestamp
            lastFingerPresent = true
            samples.addLast(red)
            while (samples.size > 450) samples.removeFirst()
            framesSinceEstimate++

            if (samples.size < 240) {
                onUpdate(PpgFrameState(
                    status = "Keep still — collecting pulse signal",
                    progress = (samples.size.toFloat() / 240).coerceIn(0f, 1f),
                    sampleCount = samples.size
                ))
                return
            }
            if (framesSinceEstimate >= 60) {
                framesSinceEstimate = 0
                val estimate = estimator.estimate(samples.toList())
                if (estimate != null && estimate.quality.name == "USABLE") {
                    onUpdate(PpgFrameState(
                        status = "Signal detected — hold still",
                        bpm = estimate.bpm,
                        confidence = estimate.confidence,
                        progress = 1f,
                        sampleCount = samples.size
                    ))
                } else {
                    onUpdate(PpgFrameState(
                        status = "Signal is unstable — adjust fingertip pressure and stay still",
                        progress = 1f,
                        sampleCount = samples.size
                    ))
                }
            }
        } catch (_: IllegalStateException) {
            onUpdate(PpgFrameState(status = "Camera frame unavailable — retrying"))
        } finally {
            image.close()
        }
    }

    private fun reset() {
        samples.clear()
        lastSampleNs = 0L
        framesSinceEstimate = 0
    }
}

data class PpgFrameState(
    val status: String = "Ready",
    val bpm: Int? = null,
    val confidence: Double = 0.0,
    val progress: Float = 0f,
    val sampleCount: Int = 0
)
