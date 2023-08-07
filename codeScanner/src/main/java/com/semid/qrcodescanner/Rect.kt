package com.semid.qrcodescanner

internal class Rect(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    override fun hashCode(): Int {
        return 31 * (31 * (31 * left + top) + right) + bottom
    }

    override fun equals(obj: Any?): Boolean {
        return when {
            obj === this -> {
                true
            }
            obj is Rect -> {
                left == obj.left && top == obj.top && right == obj.right && bottom == obj.bottom
            }
            else -> {
                false
            }
        }
    }

    override fun toString(): String {
        return "[($left; $top) - ($right; $bottom)]"
    }
}