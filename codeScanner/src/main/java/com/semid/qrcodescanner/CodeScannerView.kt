package com.semid.qrcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.semid.qrcodescanner.databinding.LayoutQrCodeScannerBinding
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class CodeScannerView(context: Context, private val attrs: AttributeSet?) :
    ConstraintLayout(context, attrs) {
    private val binding by lazy {
        LayoutQrCodeScannerBinding.inflate(LayoutInflater.from(context), this)
    }

    private lateinit var lifecycleOwner: LifecycleOwner


    private val DEFAULT_MASK_COLOR = 0x77000000
    private val DEFAULT_FRAME_COLOR = Color.WHITE
    private val DEFAULT_FRAME_THICKNESS_DP = 2f
    private val DEFAULT_FRAME_ASPECT_RATIO_WIDTH = 1f
    private val DEFAULT_FRAME_ASPECT_RATIO_HEIGHT = 1f
    private val DEFAULT_FRAME_CORNER_SIZE_DP = 50f
    private val DEFAULT_FRAME_CORNERS_RADIUS_DP = 0f
    private val DEFAULT_FRAME_SIZE = 0.75f

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private val screenAspectRatio: Int
        get() {
            val metrics = DisplayMetrics().also { binding.previewView.display.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    private var successfullyRead = false

    var onResult: (result: String) -> Unit = {}

    init {
        binding

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val density = context.resources.displayMetrics.density

        var a: TypedArray? = null
        try {
            a = context
                .obtainStyledAttributes(attrs, R.styleable.CodeScannerView)

            setMaskColor(
                a.getColor(
                    R.styleable.CodeScannerView_maskColor,
                    DEFAULT_MASK_COLOR
                )
            )
            setFrameColor(
                a.getColor(
                    R.styleable.CodeScannerView_frameColor,
                    DEFAULT_FRAME_COLOR
                )
            )
            setFrameThickness(
                a.getDimensionPixelOffset(
                    R.styleable.CodeScannerView_frameThickness,
                    Math.round(DEFAULT_FRAME_THICKNESS_DP * density).toInt()
                )
            )
            setFrameCornersSize(
                a.getDimensionPixelOffset(
                    R.styleable.CodeScannerView_frameCornersSize,
                    Math.round(DEFAULT_FRAME_CORNER_SIZE_DP * density).toInt()
                )
            )
            setFrameCornersRadius(
                a.getDimensionPixelOffset(
                    R.styleable.CodeScannerView_frameCornersRadius,
                    Math.round(DEFAULT_FRAME_CORNERS_RADIUS_DP * density).toInt()
                )
            )
            setFrameAspectRatio(
                a.getFloat(
                    R.styleable.CodeScannerView_frameAspectRatioWidth,
                    DEFAULT_FRAME_ASPECT_RATIO_WIDTH
                ),
                a.getFloat(
                    R.styleable.CodeScannerView_frameAspectRatioHeight,
                    DEFAULT_FRAME_ASPECT_RATIO_HEIGHT
                )
            )
            setFrameSize(
                a.getFloat(
                    R.styleable.CodeScannerView_frameSize,
                    DEFAULT_FRAME_SIZE
                )
            )
        } finally {
            a?.recycle()
        }
    }

    fun init(fragment: Fragment) {
        fragment.run {
            lifecycleOwner = this
            val application = requireActivity().application

            val viewModel = ViewModelProvider(
                fragment, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            ).get(CameraXViewModel::class.java)

            viewModel.cameraProviderLiveData.observe(fragment) {
                cameraProvider = it

                requestCamera(context)
            }
        }
    }

    fun init(activity: AppCompatActivity) {
        activity.run {
            lifecycleOwner = this
            val application = application

            val viewModel = ViewModelProvider(
                activity, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            ).get(CameraXViewModel::class.java)

            viewModel.cameraProviderLiveData.observe(activity) {
                cameraProvider = it

                requestCamera(context)
            }
        }
    }

    private fun requestCamera(context: Context?) {
        Dexter.withContext(context)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                    releaseCamera()
                }

                override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    permissionRequest: PermissionRequest?,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).check()
    }

    fun releaseCamera() {
        successfullyRead = false

        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    fun stopCamera() {
        previewUseCase?.let {
            cameraProvider?.unbind(it)
        }
    }

    fun readNext() {
        successfullyRead = false
    }

    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        previewUseCase!!.setSurfaceProvider(binding.previewView.surfaceProvider)

        try {
            cameraProvider!!.bindToLifecycle(
                lifecycleOwner,
                cameraSelector!!,
                previewUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message.toString())
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message.toString())
        }
    }

    private fun bindAnalyseUseCase() {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats one by one, e.g.
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build();

        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(options)

        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        // Initialize our background executor
        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase?.setAnalyzer(
            cameraExecutor,
            { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }
        )

        try {
            cameraProvider!!.bindToLifecycle(
                lifecycleOwner,
                cameraSelector!!,
                analysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            Log.e(TAG, illegalStateException.message.toString())
        } catch (illegalArgumentException: IllegalArgumentException) {
            Log.e(TAG, illegalArgumentException.message.toString())
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach {
                    val result = it.rawValue ?: ""

                    if (result.isNotEmpty() && successfullyRead.not()) {
                        successfullyRead = true

                        onResult.invoke(result)
                    }
                }
            }
            .addOnFailureListener {
                Log.e(TAG, it.message.toString())
            }.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                imageProxy.close()
            }
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
    }

    fun setFrameSize(@FloatRange(from = 0.1, to = 1.0) size: Float) {
        binding.viewFinderView.frameSize = size
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {
        private val TAG = CodeScannerView::class.java.simpleName

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}