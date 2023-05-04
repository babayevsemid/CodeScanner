package com.semid.qrcodescanner.enums

enum class LazerAnimType(val id: Int) {
    LINEAR(0),
    FADE(1);

    companion object {
        fun findById(id: Int) = values().find { it.id == id } ?: FADE
    }
}