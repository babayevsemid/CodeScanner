package com.semid.qrcodescanner

import android.app.Application
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

internal class CameraVM(application: Application) : AndroidViewModel(application) {
    private val _cameraProviderLiveData = MutableLiveData<ProcessCameraProvider>()
    val cameraProviderLiveData: LiveData<ProcessCameraProvider> get() = _cameraProviderLiveData

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener(
            {
                _cameraProviderLiveData.value = cameraProviderFuture.get()
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }
}