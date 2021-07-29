package com.semid.qrcodescanner

import android.hardware.Camera

object Utils {
    fun setFlashMode(
        parameters: Camera.Parameters,
        flashMode: String
    ) {
        if (flashMode == parameters.flashMode) {
            return
        }
        val flashModes = parameters.supportedFlashModes
        if (flashModes != null && flashModes.contains(flashMode)) {
            parameters.flashMode = flashMode
        }
    }
}