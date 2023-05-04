package com.semid.qrcodescanner.enums

enum class FrameMode(val id: Int) {
    OVAL(0),
    RECTANGLE(1);

    companion object {
        fun findById(id: Int) = values().find { it.id == id } ?: RECTANGLE
    }
}