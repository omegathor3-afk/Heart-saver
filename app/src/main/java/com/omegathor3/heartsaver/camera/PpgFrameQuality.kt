package com.omegathor3.heartsaver.camera

import kotlin.math.abs

/** Per-frame optical checks. These are engineering guardrails, not medical signal validation. */
data class FrameOptics(
    val red: Double,
    val green: Double,
    val blue: Double,
    val clippedFraction: Double
) {
    val luminance: Double get() = (red + green + blue) / 3.0
}

data class FrameQuality(
    val usable: Boolean,
    val status: String,
    val fingerDetected: Boolean
)

class PpgFrameQuality {
    private var previous: FrameOptics? = null

    fun inspect(current: FrameOptics): FrameQuality {
        if (!current.red.isFinite() || !current.green.isFinite() ||
            !current.blue.isFinite() || !current.clippedFraction.isFinite()
        ) return reject("Invalid camera data", false)

        val brightness = current.luminance
        if (brightness < MIN_BRIGHTNESS) return reject("Too dark — cover the camera and flash fully", false)
        if (brightness > MAX_BRIGHTNESS) return reject("Too much light — reposition your fingertip", false)
        if (current.clippedFraction > MAX_CLIPPED_FRACTION) {
            return reject("Image is overexposed — adjust fingertip pressure", false)
        }

        val finger = current.red >= MIN_RED &&
            current.red > current.green * RED_GREEN_RATIO &&
            current.red > current.blue * RED_BLUE_RATIO
        if (!finger) return reject("Cover the rear camera and flash with your fingertip", false)

        val old = previous
        if (old != null) {
            val luminanceJump = relativeChange(old.luminance, brightness)
            val redJump = relativeChange(old.red, current.red)
            val greenJump = relativeChange(old.green, current.green)
            val blueJump = relativeChange(old.blue, current.blue)
            val largestChannelJump = maxOf(redJump, greenJump, blueJump)
            if (luminanceJump > MAX_LUMINANCE_JUMP || largestChannelJump > MAX_CHANNEL_JUMP) {
                previous = current
                return FrameQuality(false, "Movement or pressure change — hold still", true)
            }
        }
        previous = current
        return FrameQuality(true, "Collecting pulse signal", true)
    }

    fun reset() { previous = null }

    private fun reject(message: String, finger: Boolean): FrameQuality {
        previous = null
        return FrameQuality(false, message, finger)
    }

    private fun relativeChange(a: Double, b: Double): Double =
        abs(b - a) / maxOf(abs(a), abs(b), 1.0)

    companion object {
        const val MIN_BRIGHTNESS = 45.0
        const val MAX_BRIGHTNESS = 235.0
        const val MIN_RED = 65.0
        const val RED_GREEN_RATIO = 1.04
        const val RED_BLUE_RATIO = 1.04
        const val MAX_CLIPPED_FRACTION = 0.30
        const val MAX_LUMINANCE_JUMP = 0.18
        const val MAX_CHANNEL_JUMP = 0.22
    }
}
