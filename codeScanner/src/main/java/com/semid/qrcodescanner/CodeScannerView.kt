package com.semid.qrcodescanner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.semid.qrcodescanner.databinding.LayoutQrCodeScannerBinding
import kotlin.math.roundToInt


class CodeScannerView(context: Context, private val attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding by lazy {
        LayoutQrCodeScannerBinding.inflate(LayoutInflater.from(context), this)
    }
    var lazerAnim = 0
    var lazerAnimDuration = 0

    var cameraPermission: (granted: Boolean) -> Unit = {}
    var onResult: (result: String) -> Unit = {}

    fun init(fragment: Fragment) {
        binding.previewView.init(fragment)
    }

    fun init(activity: AppCompatActivity) {
        binding.previewView.init(activity)
    }

    fun readNext() {
        binding.previewView.readNext()
    }

    fun setBarcodeFormats(formats: List<BarcodeFormat>) {
        binding.previewView.setBarcodeFormats(formats)
    }

    fun enableTorch(enable: Boolean) {
        binding.previewView.enableTorch(enable)
    }

    fun isEnabledTorch() = binding.previewView.isEnabledTorch()

    init {
        binding.previewView.onResult = { onResult.invoke(it) }
        binding.previewView.cameraPermission = {
            startLazerAnim(false)
            cameraPermission.invoke(it)
        }

        initAttrs()
    }

    private fun initAttrs() {
        val density = context.resources.displayMetrics.density

        val a = context.obtainStyledAttributes(attrs, R.styleable.CodeScannerView)

        setMaskColor(
            a.getColor(R.styleable.CodeScannerView_csvMaskColor, DEFAULT_MASK_COLOR)
        )
        setLazerColor(
            a.getColor(R.styleable.CodeScannerView_csvLazerColor, DEFAULT_LAZER_COLOR)
        )
        setFrameColor(
            a.getColor(R.styleable.CodeScannerView_csvFrameColor, DEFAULT_FRAME_COLOR)
        )
        setFrameThickness(
            a.getDimensionPixelOffset(
                R.styleable.CodeScannerView_csvFrameThickness,
                (DEFAULT_FRAME_THICKNESS_DP * density).roundToInt()
            )
        )
        setFrameCornersSize(
            a.getDimensionPixelOffset(
                R.styleable.CodeScannerView_csvFrameCornersSize,
                (DEFAULT_FRAME_CORNER_SIZE_DP * density).roundToInt()
            )
        )
        setFrameCornersRadius(
            a.getDimensionPixelOffset(
                R.styleable.CodeScannerView_csvFrameCornersRadius,
                (DEFAULT_FRAME_CORNERS_RADIUS_DP * density).roundToInt()
            )
        )
        setFrameAspectRatio(
            a.getFloat(
                R.styleable.CodeScannerView_csvFrameAspectRatioWidth,
                DEFAULT_FRAME_ASPECT_RATIO_WIDTH
            ),
            a.getFloat(
                R.styleable.CodeScannerView_csvFrameAspectRatioHeight,
                DEFAULT_FRAME_ASPECT_RATIO_HEIGHT
            )
        )
        setFrameSize(a.getFloat(R.styleable.CodeScannerView_csvFrameSize, DEFAULT_FRAME_SIZE))
        setLazerVisible(a.getBoolean(R.styleable.CodeScannerView_csvLazerVisible, true))
        setVibratorDuration(
            a.getInt(
                R.styleable.CodeScannerView_csvVibratorDuration, DEFAULT_VIBRATOR_DURATION
            )
        )
        setLazerHeight(
            a.getDimensionPixelOffset(
                R.styleable.CodeScannerView_csvLazerHeight,
                (DEFAULT_LAZER_HEIGHT * density).roundToInt()
            )
        )
        lazerAnim = a.getInt(R.styleable.CodeScannerView_csvLazerAnim, 0)
        lazerAnimDuration =
            a.getInt(R.styleable.CodeScannerView_csvLazerAnimDuration, DEFAULT_LAZER_DURATION)

        a.recycle()
    }

    fun setMaskColor(@ColorInt color: Int) {
        binding.viewFinderView.maskColor = color
    }

    fun setFrameColor(@ColorInt color: Int) {
        binding.viewFinderView.frameColor = color
    }

    fun setFrameThickness(@Px thickness: Int) {
        binding.viewFinderView.frameThickness = thickness
    }

    fun setLazerHeight(@Px height: Int) {
        val lp = binding.lazerView.layoutParams as FrameLayout.LayoutParams
        lp.height = height
        binding.lazerView.layoutParams = lp
    }

    fun setFrameCornersSize(@Px size: Int) {
        binding.viewFinderView.frameCornersSize = size
    }

    fun setFrameCornersRadius(@Px radius: Int) {
        binding.viewFinderView.frameCornersRadius = radius
    }

    fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float
    ) {
        binding.viewFinderView.setFrameAspectRatio(ratioWidth, ratioHeight)

        val lp = binding.lazerViewGroup.layoutParams as LayoutParams
        lp.dimensionRatio = "$ratioWidth:$ratioHeight"
        binding.lazerViewGroup.layoutParams = lp
    }

    fun setFrameSize(@FloatRange(from = 0.1, to = 1.0) size: Float) {
        binding.viewFinderView.frameSize = size

        val lp = binding.lazerViewGroup.layoutParams as LayoutParams
        lp.matchConstraintPercentWidth = size
        binding.lazerViewGroup.layoutParams = lp
    }

    fun setLazerColor(@ColorInt color: Int) {
        binding.lazerView.setBackgroundColor(color)
    }

    fun setLazerVisible(visible: Boolean) {
        binding.lazerView.isVisible = visible
    }

    fun setVibratorDuration(duration: Int) {
        binding.previewView.setVibratorDuration(duration)
    }


    private fun startLazerAnim(toTop: Boolean) {
        binding.lazerView.animate().setListener(null)

        if (lazerAnim == 0) {
            val animY: Float = binding.lazerViewGroup.height.toFloat() / 2
            val translationY = if (toTop) -animY else animY

            val firstTime = binding.lazerView.translationY == 0f

            binding.lazerView.animate()
                .setDuration(if (firstTime) lazerAnimDuration.toLong() / 2 else lazerAnimDuration.toLong())
                .translationY(translationY)
                .setInterpolator(LinearInterpolator())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        startLazerAnim(!toTop)
                    }
                })
        } else {
            binding.lazerView.animate()
                .setDuration(lazerAnimDuration.toLong())
                .alpha(if (binding.lazerView.alpha > 0) 0f else 0.8f)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        startLazerAnim(true)
                    }
                })
        }
    }

    companion object {
        private const val DEFAULT_MASK_COLOR = 0x77000000
        private const val DEFAULT_FRAME_COLOR = Color.WHITE
        private const val DEFAULT_FRAME_THICKNESS_DP = 4f
        private const val DEFAULT_FRAME_ASPECT_RATIO_WIDTH = 1f
        private const val DEFAULT_FRAME_ASPECT_RATIO_HEIGHT = 1f
        private const val DEFAULT_FRAME_CORNER_SIZE_DP = 40f
        private const val DEFAULT_FRAME_CORNERS_RADIUS_DP = 10f
        private const val DEFAULT_FRAME_SIZE = 0.75f
        private const val DEFAULT_VIBRATOR_DURATION = 100
        private const val DEFAULT_LAZER_COLOR = Color.WHITE
        private const val DEFAULT_LAZER_HEIGHT = 1f
        private const val DEFAULT_LAZER_DURATION = 2000
    }
}