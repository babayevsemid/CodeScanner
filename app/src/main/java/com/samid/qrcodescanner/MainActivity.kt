package com.samid.qrcodescanner

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
        binding.scanner.enableNegativeScan(true)
        binding.scanner.setCodeValidLength((13..30).toList())

        binding.scanner.permissionMessageCanceled = {
            Log.e("permissionMessag", "$it")
        }

        binding.scanner.torchState = {
            binding.flashBtn.icon =
                ContextCompat.getDrawable(
                    applicationContext,
                    if (it) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                )
        }


        binding.scanner.onResult = {
            if (it.isNotEmpty()) {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    binding.scanner.readNext()
                }, 1000)
            }
        }

        binding.scanner.onResultFromFile = {
            if (it.isNotEmpty()) {
                Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    binding.scanner.readNext()
                }, 1000)
            }
        }

        binding.flashBtn.setOnClickListener {
//            startActivity(Intent(this, TestFragmentActivity::class.java))

            binding.scanner.changeTorchState()
        }
    }
}