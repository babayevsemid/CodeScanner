package com.semid.qrcodescanner

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData


class CameraXViewModel(application: Application) : AndroidViewModel(application) {
    private val _cameraProviderLiveData = MutableLiveData<ProcessCameraProvider>()
      val cameraProviderLiveData: LiveData<ProcessCameraProvider> get() = _cameraProviderLiveData

    init {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(getApplication())
        cameraProviderFuture.addListener(
            {
                try {
                    _cameraProviderLiveData.value = cameraProviderFuture.get()
                } catch (e: Exception) {
                    Log.e(TAG, "Unhandled exception", e)
                }
            },
            ContextCompat.getMainExecutor(getApplication())
        )
    }

    companion object {
        private const val TAG = "CameraXViewModel"
    }
}