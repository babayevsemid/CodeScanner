/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.semid.qrcodescanner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import com.semid.qrcodescanner.enums.FrameMode
import kotlin.math.roundToInt

internal class ViewFinderView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private val mMaskPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mFramePaint: Paint
    private val mPath: Path
    private var frameRect: Rect? = null

    private var mFrameMode = FrameMode.RECTANGLE
    private var mFrameCornersSize = 0
    private var mFrameCornersRadius = 0
    private var mFrameRatioWidth = 1f
    private var mFrameRatioHeight = 1f
    private var mFrameSize = 0.75f
    private var mFrameThicknessMargin = 0.75f

    override fun onDraw(canvas: Canvas) {
        val frame = frameRect ?: return
        val width = width
        val height = height
        val top = frame.top.toFloat()
        val left = frame.left.toFloat()
        val right = frame.right.toFloat()
        val bottom = frame.bottom.toFloat()
        val frameCornersSize = mFrameCornersSize.toFloat()
        val frameCornersRadius = mFrameCornersRadius.toFloat()
        val path = mPath

        if (mFrameMode == FrameMode.OVAL) {
            val frameRadius = (context.resources.displayMetrics.widthPixels * mFrameSize) / 2f

            path.reset()
            path.fillType = Path.FillType.INVERSE_EVEN_ODD
            val centerX = getWidth() / 2f
            val centerY = getHeight() / 2f

            path.addCircle(centerX, centerY, frameRadius, Path.Direction.CW)
            canvas.drawPath(path, mMaskPaint)

            path.reset()
            path.fillType = Path.FillType.EVEN_ODD
            path.addCircle(
                centerX,
                centerY,
                frameRadius + frameThicknessMargin,
                Path.Direction.CW
            )

            canvas.drawPath(path, mFramePaint)
        } else {
            val normalizedRadius =
                Math.min(frameCornersRadius, Math.max(frameCornersSize - 1, 0f))
            path.reset()
            path.moveTo(left, top + normalizedRadius)
            path.quadTo(left, top, left + normalizedRadius, top)
            path.lineTo(right - normalizedRadius, top)
            path.quadTo(right, top, right, top + normalizedRadius)
            path.lineTo(right, bottom - normalizedRadius)
            path.quadTo(right, bottom, right - normalizedRadius, bottom)
            path.lineTo(left + normalizedRadius, bottom)
            path.quadTo(left, bottom, left, bottom - normalizedRadius)
            path.lineTo(left, top + normalizedRadius)
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat(), 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.lineTo(0f, 0f)
            canvas.drawPath(path, mMaskPaint)


            path.reset()
            path.moveTo(
                left - frameThicknessMargin,
                top + frameCornersSize - frameThicknessMargin
            )
            path.lineTo(
                left - frameThicknessMargin,
                top + normalizedRadius - frameThicknessMargin
            )
            path.quadTo(
                left - frameThicknessMargin,
                top - frameThicknessMargin,
                left + normalizedRadius - frameThicknessMargin,
                top - frameThicknessMargin
            )
            path.lineTo(
                left + frameCornersSize - frameThicknessMargin,
                top - frameThicknessMargin
            )
            path.moveTo(
                right - frameCornersSize + frameThicknessMargin,
                top - frameThicknessMargin
            )
            path.lineTo(
                right - normalizedRadius + frameThicknessMargin,
                top - frameThicknessMargin
            )
            path.quadTo(
                right + frameThicknessMargin,
                top - frameThicknessMargin,
                right + frameThicknessMargin,
                top + normalizedRadius - frameThicknessMargin
            )
            path.lineTo(
                right + frameThicknessMargin,
                top + frameCornersSize - frameThicknessMargin
            )
            path.moveTo(
                right + frameThicknessMargin,
                bottom - frameCornersSize + frameThicknessMargin
            )
            path.lineTo(
                right + frameThicknessMargin,
                bottom - normalizedRadius + frameThicknessMargin
            )
            path.quadTo(
                right + frameThicknessMargin,
                bottom + frameThicknessMargin,
                right - normalizedRadius + frameThicknessMargin,
                bottom + frameThicknessMargin
            )
            path.lineTo(
                right - frameCornersSize + frameThicknessMargin,
                bottom + frameThicknessMargin
            )
            path.moveTo(
                left + frameCornersSize - frameThicknessMargin,
                bottom + frameThicknessMargin
            )
            path.lineTo(
                left + normalizedRadius - frameThicknessMargin,
                bottom + frameThicknessMargin
            )
            path.quadTo(
                left - frameThicknessMargin,
                bottom + frameThicknessMargin,
                left - frameThicknessMargin,
                bottom - normalizedRadius + frameThicknessMargin
            )
            path.lineTo(
                left - frameThicknessMargin,
                bottom + frameThicknessMargin - frameCornersSize
            )
            canvas.drawPath(path, mFramePaint)
        }
    }

    override fun onLayout(
        changed: Boolean, left: Int, top: Int, right: Int,
        bottom: Int
    ) {
        invalidateFrameRect(right - left, bottom - top)
    }

    fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float
    ) {
        mFrameRatioWidth = ratioWidth
        mFrameRatioHeight = ratioHeight
        invalidateFrameRect()
        if (isLaidOut) {
            invalidate()
        }
    }

    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioWidth: Float
        get() = mFrameRatioWidth
        set(ratioWidth) {
            mFrameRatioWidth = ratioWidth
            invalidateFrameRect()
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioHeight: Float
        get() = mFrameRatioHeight
        set(ratioHeight) {
            mFrameRatioHeight = ratioHeight
            invalidateFrameRect()
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:ColorInt
    var maskColor: Int
        get() = mMaskPaint.color
        set(color) {
            mMaskPaint.color = color
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:ColorInt
    var frameColor: Int
        get() = mFramePaint.color
        set(color) {
            mFramePaint.color = color
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameThickness: Int
        get() = mFramePaint.strokeWidth.toInt()
        set(thickness) {
            mFramePaint.strokeWidth = thickness.toFloat()
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameThicknessMargin: Float
        get() = mFrameThicknessMargin
        set(size) {
            mFrameThicknessMargin = size * 2
            invalidateFrameRect()

            if (isLaidOut) {
                invalidate()
            }
        }

    var frameMode: FrameMode
        get() = mFrameMode
        set(size) {
            mFrameMode = size
            invalidateFrameRect()

            if (isLaidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameCornersSize: Int
        get() = mFrameCornersSize
        set(size) {
            mFrameCornersSize = size
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameCornersRadius: Int
        get() = mFrameCornersRadius
        set(radius) {
            mFrameCornersRadius = radius
            if (isLaidOut) {
                invalidate()
            }
        }

    @get:FloatRange(from = 0.1, to = 1.0)
    var frameSize: Float
        get() = mFrameSize
        set(size) {
            mFrameSize = size
            invalidateFrameRect()
            if (isLaidOut) {
                invalidate()
            }
        }

    private fun invalidateFrameRect(width: Int = getWidth(), height: Int = getHeight()) {
        if (width > 0 && height > 0) {
            val viewAR = width.toFloat() / height.toFloat()
            val frameAR = mFrameRatioWidth / mFrameRatioHeight
            val frameWidth: Int
            val frameHeight: Int
            if (viewAR <= frameAR) {
                frameWidth = (width * mFrameSize).roundToInt()
                frameHeight = (frameWidth / frameAR).roundToInt()
            } else {
                frameHeight = (height * mFrameSize).roundToInt()
                frameWidth = (frameHeight * frameAR).roundToInt()
            }
            val frameLeft = (width - frameWidth) / 2
            val frameTop = (height - frameHeight) / 2
            frameRect = Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)
        }
    }

    fun getRectHeight(): Float {
        val height = width * mFrameRatioHeight / mFrameRatioWidth

        Log.e("height", height.toString())
        return height
    }

    init {
        mMaskPaint.style = Paint.Style.FILL
        mFramePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mFramePaint.style = Paint.Style.STROKE
        val path = Path()
        path.fillType = Path.FillType.EVEN_ODD
        mPath = path
    }
}