package com.semid.qrcodescanner

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface.ROTATION_0
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import com.semid.qrcodescanner.Utils.applyNegativeEffect
import com.semid.qrcodescanner.Utils.toBitmap
import com.semid.qrcodescanner.databinding.LayoutCameraPreviewViewBinding
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal class CameraPreviewView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs),
    LifecycleObserver {
    private val binding by lazy {
        LayoutCameraPreviewViewBinding.inflate(LayoutInflater.from(context), this)
    }

    private lateinit var lifecycleOwner: LifecycleOwner
    private val results = mutableListOf<String>()

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null
    private var previewUseCase: Preview? = null
    private var analysisUseCase: ImageAnalysis? = null
    private var negativeUseCase: ImageAnalysis? = null

    private var deniedModel = BarcodeDeniedModel()
    private var deniedType = BarcodeDeniedType.SNACK_BAR
    private var barcodeFormats = intArrayOf(Barcode.FORMAT_ALL_FORMATS)
    private var reCheckPermission = false
    private var successfullyRead = false
    private var vibratorDuration = 0
    private var snackBar: Snackbar? = null
    private var codeValidLength = listOf<Int>()
    private var enableNegativeScan = false
    private var accuracyDuration = 0

    private val screenAspectRatio: Int
        get() {
            val metrics = context.resources.displayMetrics
            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    var torchState: (isON: Boolean) -> Unit = {}
    var cameraPermission: (granted: Boolean) -> Unit = {}
    var onResult: (result: String) -> Unit = {}
    var onResultFromFile: (result: String) -> Unit = {}
    var permissionMessageCanceled: (autoCancel: Boolean) -> Unit = {}

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
            )[CameraVM::class.java]

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
            )[CameraVM::class.java]

            viewModel.cameraProviderLiveData.observe(activity) {
                cameraProvider = it

                requestCamera(context)
            }
        }
    }

    fun requestCamera(context: Context?) {
        if (context == null || cameraProvider == null) {
            return
        }

        reCheckPermission = false

        val permission = object : PermissionListener {
            override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                releaseCamera()
                cameraPermission.invoke(true)
            }

            override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {
                reCheckPermission = true
                cameraPermission.invoke(false)

                showDeniedMessage()
            }

            override fun onPermissionRationaleShouldBeShown(
                permissionRequest: PermissionRequest?,
                permissionToken: PermissionToken
            ) {
                permissionToken.continuePermissionRequest()
            }
        }

        Dexter.withContext(context)
            .withPermission(Manifest.permission.CAMERA)
            .withListener(permission)
            .check()
    }

    private fun showDeniedMessage() {
        val titleText =
            deniedModel.title ?: context.getString(R.string.camera_access_title)
        val settingsText =
            deniedModel.settingButtonText ?: context.getString(R.string.settings)
        val cancelText =
            deniedModel.cancelButtonText ?: context.getString(R.string.cancel)

        when (deniedType) {
            BarcodeDeniedType.SNACK_BAR -> {
                snackBar = Snackbar.make(this, titleText, deniedModel.snackBarDuration)
                    .setAction(settingsText) {
                        showAppSettings()
                    }.addCallback(object : Snackbar.Callback() {
                        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                            permissionMessageCanceled(event != 1)
                        }
                    })
                snackBar?.show()
            }

            BarcodeDeniedType.DIALOG -> {
                AlertDialog.Builder(context)
                    .setTitle(deniedModel.title ?: context.getString(R.string.camera_access_title))
                    .setPositiveButton(settingsText) { d, _ ->
                        showAppSettings()
                        permissionMessageCanceled(false)
                        d.dismiss()
                    }.setNegativeButton(cancelText) { d, _ ->
                        d.cancel()
                    }.setOnDismissListener {
                    }.setOnCancelListener {
                        permissionMessageCanceled(true)
                    }.create().show()
            }

            else -> {
            }
        }
    }

    private fun showAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        startActivity(context, intent, null)
    }

    fun setBarcodeFormats(formats: List<BarcodeFormat>) {
        barcodeFormats = formats.map { it.id }.toIntArray()
    }

    fun setDeniedType(deniedType: BarcodeDeniedType) {
        this.deniedType = deniedType
    }

    fun setCodeValidLength(list: List<Int>) {
        this.codeValidLength = list
    }

    fun dismissSnackBar() {
        snackBar?.dismiss()
    }

    fun setDeniedModel(deniedModel: BarcodeDeniedModel) {
        this.deniedModel = deniedModel
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
            .setTargetRotation(ROTATION_0)
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
        if (cameraProvider == null)
            return

        if (analysisUseCase != null)
            cameraProvider?.unbind(analysisUseCase)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(barcodeFormats[0], *barcodeFormats)
            .build()

        val barcodeScanner = BarcodeScanning.getClient(options)

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(ROTATION_0)
            .build()

        analysisUseCase?.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
            if (enableNegativeScan) {
                processImageNegativeProxy(barcodeScanner, imageProxy)
            }else{
                processImageProxy(barcodeScanner, imageProxy)
            }
        }

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

    private var timer: CountDownTimer? = null

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees, Matrix())

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }?.let { barcode ->
                        val result = barcode.rawValue.orEmpty()

                        if (successfullyRead.not() &&
                            (codeValidLength.isEmpty() || codeValidLength.contains(result.length))
                        ) {
                            val startTimer = results.isEmpty()
                            results.add(result)

                            if (startTimer) {
                                timer = object : CountDownTimer(accuracyDuration.toLong(), 1000) {
                                    override fun onTick(p0: Long) {}

                                    override fun onFinish() {
                                        successfullyRead = true

                                        vibrate()

                                        onResult.invoke(
                                            results.groupingBy { it }
                                                .eachCount()
                                                .maxByOrNull { it.value }
                                                ?.key.orEmpty().also {
                                                    results.clear()
                                                }
                                        )
                                    }
                                }.start()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    onResult.invoke("")
                }.addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageNegativeProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        imageProxy.toBitmap().let { bitmap ->
            val inputImage = InputImage.fromBitmap(bitmap.applyNegativeEffect(), 0)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull { !it.rawValue.isNullOrEmpty() }?.let { barcode ->
                        val result = barcode.rawValue.orEmpty()

                        if (successfullyRead.not() &&
                            (codeValidLength.isEmpty() || codeValidLength.contains(result.length))
                        ) {
                            val startTimer = results.isEmpty()
                            results.add(result)

                            if (startTimer) {
                                object : CountDownTimer(accuracyDuration.toLong(), 1000) {
                                    override fun onTick(p0: Long) {}

                                    override fun onFinish() {
                                        successfullyRead = true

                                        vibrate()

                                        onResult.invoke(
                                            results.groupingBy { it }
                                                .eachCount()
                                                .maxByOrNull { it.value }
                                                ?.key.orEmpty().also {
                                                    results.clear()
                                                }
                                        )
                                    }
                                }.start()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    onResult.invoke("")
                }.addOnCompleteListener {
                    imageProxy.close()
                }
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
        bitmap?.applyNegativeEffect()?.let {
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

    fun setAccuracyDuration(duration: Int) {
        accuracyDuration = duration
    }

    fun enableNegativeScan(enable: Boolean) {
        enableNegativeScan = enable
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun checkTorchState() {
        torchState.invoke(isEnabledTorch())
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    private fun reCheckPermission() {
        if (reCheckPermission) {
//            if (cameraPermissionIsGranted()) {
            requestCamera(context)
//            } else {
//                showDeniedMessage()
//            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onStop() {
        dismissSnackBar()
    }

    private fun cameraPermissionIsGranted() =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {
        private val TAG = "CodeScannerView"

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }
}