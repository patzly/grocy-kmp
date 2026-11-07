package com.patrickzedler.grocy.core.scanner

/** An image in the NV21 format of Android's cameras. */
class Nv21Image(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
)

/**
 * The frame in grayscale with the dots of dot matrix prints joined into strokes, for text
 * recognition engines, which read such prints badly. Not turned upright, like the frame.
 */
fun CameraFrame.dotsJoined(): Nv21Image {
    val luminance = image.toLuminanceImage()
    val blurred = boxBlurred(luminance.bytes, luminance.width, luminance.height, DOT_RADIUS)
    return Nv21Image(
        bytes = grayscaleNv21(blurred, luminance.width, luminance.height),
        width = luminance.width,
        height = luminance.height,
    )
}

// At 1280 × 720 a printed date's dots lie up to about 3 pixels apart
private const val DOT_RADIUS = 2
