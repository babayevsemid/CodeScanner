package com.samid.qrcodescanner

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.samid.qrcodescanner.databinding.ActivityMainBinding
import com.semid.filechooser.FileChooserActivity
import com.semid.filechooser.FileTypeEnum
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

        binding.scanner.torchState = {
            binding.flashBtn.icon =
                ContextCompat.getDrawable(
                    applicationContext,
                    if (it) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                )
        }

        binding.scanner.onResult = {
            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()

            Handler(Looper.myLooper()!!).postDelayed({
                binding.scanner.readNext()
            }, 1000)
        }

        binding.scanner.onResultFromFile = {
            Toast.makeText(applicationContext, it, Toast.LENGTH_SHORT).show()

            Handler(Looper.myLooper()!!).postDelayed({
                binding.scanner.readNext()
            }, 1000)
        }

        val fileChooser = FileChooserActivity(this)
        fileChooser.fileLiveData
            .observe(this, {
                binding.scanner.scanFromPath(it.path)
            })
        fileChooser.requestFile(FileTypeEnum.CHOOSE_PHOTO)

        binding.flashBtn.setOnClickListener {
            startActivity(Intent(this, TestFragmentActivity::class.java))

            binding.scanner.changeTorchState()
        }
    }
}