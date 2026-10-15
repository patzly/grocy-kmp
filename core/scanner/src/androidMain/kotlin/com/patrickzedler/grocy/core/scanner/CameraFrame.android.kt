package com.patrickzedler.grocy.core.scanner

import androidx.camera.core.ImageProxy

/** The analysis image of CameraX; [image] stays owned by the scanner, which closes it. */
actual class CameraFrame(val image: ImageProxy)
