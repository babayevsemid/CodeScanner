package com.semid.qrcodescanner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
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
import com.semid.qrcodescanner.Utils.applyNegativeEffect
import com.semid.qrcodescanner.databinding.LayoutQrCodeScannerBinding
import com.semid.qrcodescanner.enums.FrameMode
import com.semid.qrcodescanner.enums.LazerAnimType
import kotlin.math.roundToInt


class CodeScannerView(context: Context, private val attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {

    private val binding by lazy {
        LayoutQrCodeScannerBinding.inflate(LayoutInflater.from(context), this)
    }
    private var lazerAnim = LazerAnimType.FADE
    private var lazerAnimDuration = 0

    @JvmField
    var torchState: (isON: Boolean) -> Unit = {}

    @JvmField
    var cameraPermission: (granted: Boolean) -> Unit = {}

    @JvmField
    var onResult: (result: String) -> Unit = {}

    @JvmField
    var onResultFromFile: (result: String) -> Unit = {}

    @JvmField
    var permissionMessageCanceled: (autoCancel: Boolean) -> Unit = {}

    init {
        binding.previewView.torchState = { torchState.invoke(it) }
        binding.previewView.onResult = { onResult.invoke(it) }
        binding.previewView.onResultFromFile = { onResultFromFile.invoke(it) }

        binding.previewView.cameraPermission = {
            startLazerAnim(false)
            cameraPermission.invoke(it)
        }

        binding.previewView.permissionMessageCanceled = { permissionMessageCanceled.invoke(it) }

        initAttrs()
    }

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

    fun setCodeValidLength(list: List<Int>) {
        binding.previewView.setCodeValidLength(list)
    }

    fun enableNegativeScan(enable: Boolean) {
        binding.previewView.enableNegativeScan(enable = enable)
    }

    fun scanFromUri(uri: Uri?) {
        binding.previewView.scanFromUri(uri)
    }

    fun scanFromPath(path: String?) {
        binding.previewView.scanFromPath(path)
    }

    fun scanFromBitmap(bitmap: Bitmap?) {
        binding.previewView.scanFromBitmap(bitmap)
    }

    fun requestCameraPermission(context: Context) {
        binding.previewView.requestCamera(context)
    }

    fun vibrate() {
        binding.previewView.vibrate()
    }

    fun enableTorch(enable: Boolean) {
        binding.previewView.enableTorch(enable)
    }

    fun changeTorchState() {
        binding.previewView.enableTorch(!isEnabledTorch())
    }

    fun isEnabledTorch() = binding.previewView.isEnabledTorch()


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
        setFrameThicknessMargin(
            a.getDimensionPixelOffset(
                R.styleable.CodeScannerView_csvFrameThicknessMargin,
                (DEFAULT_FRAME_THICKNESS_MARGIN_DP * density).roundToInt()
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
        setDeniedType(
            BarcodeDeniedType.find(
                a.getInt(
                    R.styleable.CodeScannerView_csvDeniedType,
                    BarcodeDeniedType.SNACK_BAR.id
                )
            )
        )
        setFrameMode(
            FrameMode.findById(
                a.getInt(
                    R.styleable.CodeScannerView_csvFrameMode,
                    FrameMode.RECTANGLE.id
                )
            )
        )

        lazerAnim =
            LazerAnimType.findById(
                a.getInt(
                    R.styleable.CodeScannerView_csvLazerAnim,
                    LazerAnimType.FADE.id
                )
            )
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

    fun setFrameMode(mode: FrameMode) {
        binding.viewFinderView.frameMode = mode
    }

    fun setFrameThickness(@Px thickness: Int) {
        binding.viewFinderView.frameThickness = thickness
    }

    fun setFrameThicknessMargin(@Px margin: Int) {
        binding.viewFinderView.frameThicknessMargin = margin.toFloat()
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

    fun setAccuracyDuration(duration: Int) {
        binding.previewView.setAccuracyDuration(duration)
    }

    fun setDeniedType(deniedType: BarcodeDeniedType) {
        binding.previewView.setDeniedType(deniedType)
    }

    fun setDeniedModel(deniedModel: BarcodeDeniedModel) {
        binding.previewView.setDeniedModel(deniedModel)
    }

    private fun startLazerAnim(toTop: Boolean) {
        binding.lazerView.animate().setListener(null)

        if (lazerAnim == LazerAnimType.LINEAR) {
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
        private const val DEFAULT_FRAME_THICKNESS_MARGIN_DP = 0f
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