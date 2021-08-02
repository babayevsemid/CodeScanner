package com.semid.qrcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
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
import com.semid.qrcodescanner.databinding.LayoutCameraPreviewViewBinding
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class CameraPreviewView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs),
    LifecycleObserver {
    private val binding by lazy {
        LayoutCameraPreviewViewBinding.inflate(LayoutInflater.from(context), this)
    }

    private lateinit var lifecycleOwner: LifecycleOwner

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null

    private var barcodeFormats = intArrayOf(Barcode.FORMAT_ALL_FORMATS)
    private var successfullyRead = false
    private var vibratorDuration = 0

    private val screenAspectRatio: Int
        get() {
            val metrics = DisplayMetrics().also { binding.previewView.display.getRealMetrics(it) }
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    var torchState: (isON: Boolean) -> Unit = {}
    var cameraPermission: (granted: Boolean) -> Unit = {}
    var onResult: (result: String) -> Unit = {}
    var onResultFromFile: (result: String) -> Unit = {}

    init {
        binding

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
    }

    fun init(fragment: Fragment) {
        fragment.run {
            lifecycleOwner = this
            lifecycle.addObserver(this@CameraPreviewView)

            val application = requireActivity().application

            val viewModel = ViewModelProvider(
                fragment, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            ).get(CameraVM::class.java)

            viewModel.cameraProviderLiveData.observe(fragment) {
                cameraProvider = it

                requestCamera(context)
            }
        }
    }

    fun init(activity: AppCompatActivity) {
        activity.run {
            lifecycleOwner = this
            lifecycle.addObserver(this@CameraPreviewView)

            val application = application

            val viewModel = ViewModelProvider(
                activity, ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            ).get(CameraVM::class.java)

            viewModel.cameraProviderLiveData.observe(activity) {
                cameraProvider = it

                requestCamera(context)
            }
        }
    }

    fun requestCamera(context: Context?) {
        Dexter.withContext(context)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                    releaseCamera()
                    cameraPermission.invoke(true)
                }

                override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {
                    cameraPermission.invoke(false)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissionRequest: PermissionRequest?,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).check()
    }

    fun setBarcodeFormats(formats: List<BarcodeFormat>) {
        barcodeFormats = formats.map { it.id }.toIntArray()
    }

    private fun releaseCamera() {
        successfullyRead = false

        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    fun readNext() {
        successfullyRead = false
    }

    @SuppressLint("RestrictedApi")
    private fun bindPreviewUseCase() {
        if (cameraProvider == null) {
            return
        }

        if (previewUseCase != null)
            cameraProvider?.unbind(previewUseCase)

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(binding.previewView.display.rotation)
            .build()
        previewUseCase?.setSurfaceProvider(binding.previewView.surfaceProvider)

        try {
            cameraProvider?.bindToLifecycle(
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
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(barcodeFormats[0], *barcodeFormats)
            .build()

        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(options)

        if (cameraProvider == null)
            return

        if (analysisUseCase != null)
            cameraProvider!!.unbind(analysisUseCase)

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(binding.previewView.display.rotation)
            .build()

        // Initialize our background executor
        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase?.setAnalyzer(
            cameraExecutor, { imageProxy ->
                processImageProxy(barcodeScanner, imageProxy)
            }
        )

        try {
            cameraProvider?.bindToLifecycle(
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

                        vibrate()
                        onResult.invoke(result)
                    }
                }
            }
            .addOnFailureListener {
                onResult.invoke("")
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun scanFromUri(uri: Uri?) {
        uri?.let {
            scanFromInputImage(InputImage.fromFilePath(context, uri))
        }
    }

    fun scanFromPath(path: String?) {
        path?.let {
            val uri = Uri.fromFile(File(it))
            scanFromInputImage(InputImage.fromFilePath(context, uri))
        }
    }

    fun scanFromBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            scanFromInputImage(InputImage.fromBitmap(bitmap, 0))
        }
    }

    private fun scanFromInputImage(image: InputImage) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(barcodeFormats[0], *barcodeFormats)
            .build()

        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.size > 0) {
                    for (barcode in barcodes) {
                        val result = barcode.rawValue ?: ""

                        if (result.isNotEmpty() && successfullyRead.not()) {
                            successfullyRead = true

                            vibrate()
                            onResultFromFile.invoke(result)
                        }
                    }
                } else
                    onResultFromFile.invoke("")
            }
            .addOnFailureListener {
                onResultFromFile.invoke("")
            }
    }

    @SuppressLint("RestrictedApi")
    fun enableTorch(enable: Boolean) {
        if (previewUseCase?.camera?.cameraInfo?.hasFlashUnit() == true)
            previewUseCase?.camera?.cameraControl?.enableTorch(enable)

        checkTorchState()
    }

    @SuppressLint("RestrictedApi")
    fun isEnabledTorch() = previewUseCase?.camera?.cameraInfo?.torchState?.value == TorchState.ON

    fun vibrate() {
        if (vibratorDuration > 0) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(
                        vibratorDuration.toLong(),
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibratorDuration.toLong())
            }
        }
    }

    fun setVibratorDuration(duration: Int) {
        vibratorDuration = duration
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun checkTorchState() {
        torchState.invoke(isEnabledTorch())
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