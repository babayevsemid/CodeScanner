package com.semid.qrcodescanner

import com.google.android.material.snackbar.Snackbar

data class BarcodeDeniedModel(
    val title: String? = null,
    val message: String? = null,
    val snackBarDuration: Int = Snackbar.LENGTH_INDEFINITE,
    val cancelButtonText: String? = null,
    val settingButtonText: String? = null,
)