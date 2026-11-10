package com.omegathor3.heartsaver.camera

import kotlin.math.abs

data class FrameOptics(
    val red: Double,
    val green: Double,
    val blue: Double,
    val clippedFraction: Double,
    val fingerPixelFraction: Double = 1.0
) {
    val luminance: Double get() = (red + green + blue) / 3.0
}

data class FrameQuality(
    val usable: Boolean,
    val status: String,
    val fingerDetected: Boolean
)

/** Optical guardrails only; this is not clinical signal validation. */
class PpgFrameQuality {
    private var previous: FrameOptics? = null
    private var recoveryFramesRemaining = 0

    fun inspect(current: FrameOptics): FrameQuality {
        if (
            !current.red.isFinite() || current.red !in MIN_CHANNEL..MAX_CHANNEL ||
            !current.green.isFinite() || current.green !in MIN_CHANNEL..MAX_CHANNEL ||
            !current.blue.isFinite() || current.blue !in MIN_CHANNEL..MAX_CHANNEL ||
            !current.clippedFraction.isFinite() ||
            !current.fingerPixelFraction.isFinite() ||
            current.clippedFraction !in 0.0..1.0 ||
            current.fingerPixelFraction !in 0.0..1.0
        ) {
            return reject("Invalid camera data", false)
        }

        val brightness = current.luminance
        if (!brightness.isFinite() || brightness < MIN_BRIGHTNESS) {
            return reject("Too dark — cover the camera and flash fully", false)
        }
        if (brightness > MAX_BRIGHTNESS) {
            return reject("Too much light — reposition your fingertip", false)
        }
        if (current.clippedFraction > MAX_CLIPPED_FRACTION) {
            return reject("Image is overexposed — adjust fingertip pressure", false)
        }

        val fingerDetected =
            current.red >= MIN_RED &&
                current.red > current.green * RED_GREEN_RATIO &&
                current.red > current.blue * RED_BLUE_RATIO &&
                current.fingerPixelFraction >= MIN_FINGER_PIXEL_FRACTION
        if (!fingerDetected) {
            return reject("Cover the camera evenly with your fingertip", false)
        }

        if (recoveryFramesRemaining > 0) {
            recoveryFramesRemaining--
            previous = current
            return FrameQuality(false, "Stabilizing after movement — hold still", true)
        }

        val old = previous
        if (old != null) {
            val luminanceJump = relativeChange(old.luminance, brightness)
            val largestChannelJump = maxOf(
                relativeChange(old.red, current.red),
                relativeChange(old.green, current.green),
                relativeChange(old.blue, current.blue)
            )
            if (luminanceJump > MAX_LUMINANCE_JUMP || largestChannelJump > MAX_CHANNEL_JUMP) {
                previous = current
                recoveryFramesRemaining = RECOVERY_FRAMES
                return FrameQuality(false, "Movement or pressure change — hold still", true)
            }
        }

        previous = current
        return FrameQuality(true, "Collecting pulse signal", true)
    }

    fun reset() {
        previous = null
        recoveryFramesRemaining = 0
    }

    private fun reject(message: String, fingerDetected: Boolean): FrameQuality {
        previous = null
        recoveryFramesRemaining = 0
        return FrameQuality(false, message, fingerDetected)
    }

    private fun relativeChange(a: Double, b: Double): Double =
        abs(b - a) / maxOf(abs(a), abs(b), 1.0)

    companion object {
        private const val MIN_CHANNEL = 0.0
        private const val MAX_CHANNEL = 255.0
        const val MIN_BRIGHTNESS = 45.0
        const val MAX_BRIGHTNESS = 235.0
        const val MIN_RED = 65.0
        const val RED_GREEN_RATIO = 1.04
        const val RED_BLUE_RATIO = 1.04
        const val MAX_CLIPPED_FRACTION = 0.30
        const val MIN_FINGER_PIXEL_FRACTION = 0.65
        const val MAX_LUMINANCE_JUMP = 0.18
        const val MAX_CHANNEL_JUMP = 0.22
        private const val RECOVERY_FRAMES = 2
    }
}


/**
 * Structural validation for the single RGBA_8888 plane consumed by the camera analyzer.
 * Validate the complete image extent before indexing any pixel so malformed strides
 * cannot silently turn a partial frame into a plausible optical-quality sample.
 */
internal data class PpgRgbaPlaneLayout(
    val width: Int,
    val height: Int,
    val rowStride: Int,
    val pixelStride: Int,
    val bufferLimit: Int
) {
    fun isValid(): Boolean {
        if (width < 8 || height < 8 || pixelStride.toLong() != BYTES_PER_PIXEL ||
            rowStride <= 0 || bufferLimit < 0
        ) return false

        val minimumRowBytes = width.toLong() * BYTES_PER_PIXEL
        if (rowStride.toLong() < minimumRowBytes) return false

        val requiredBytes = (height.toLong() - 1L) * rowStride.toLong() + minimumRowBytes
        return requiredBytes <= bufferLimit.toLong()
    }

    private companion object {
        const val BYTES_PER_PIXEL = 4L
    }
}
