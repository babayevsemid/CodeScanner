package com.semid.qrcodescanner

enum class BarcodeDeniedType(val id: Int) {
    NONE(0), SNACK_BAR(1), DIALOG(2);

    companion object {
        fun find(id: Int) = values().find { it.id == id } ?: NONE
    }
}