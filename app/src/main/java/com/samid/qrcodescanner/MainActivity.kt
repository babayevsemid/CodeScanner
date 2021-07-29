package com.samid.qrcodescanner

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samid.qrcodescanner.databinding.ActivityMainBinding
import com.semid.qrcodescanner.BarcodeFormat

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.scanner.init(this)
        binding.scanner.setBarcodeFormats(arrayListOf(BarcodeFormat.FORMAT_QR_CODE))

        binding.scanner.onResult = {

            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()
            Log.e("result", it)

            Handler().postDelayed({
                binding.scanner.readNext()
            }, 1000)
        }

        binding.flashBtn.setOnClickListener {
            binding.scanner.enableTorch(!binding.scanner.isEnabledTorch())
        }
    }
}