package com.samid.qrcodescanner

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.samid.qrcodescanner.databinding.FragmentBlankBinding
import com.semid.qrcodescanner.BarcodeFormat


class BlankFragment : Fragment() {
    private val binding by lazy {
        FragmentBlankBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        init()
        return binding.root
    }

    private fun init() {
        binding.scanner.init(this)
        binding.scanner.setBarcodeFormats(arrayListOf(BarcodeFormat.FORMAT_QR_CODE))

        binding.scanner.torchState = {
            binding.flashBtn.icon =
                ContextCompat.getDrawable(
                    requireContext(),
                    if (it) R.drawable.ic_flash_on else R.drawable.ic_flash_off
                )
        }

        binding.scanner.onResult = {
            if (it.isNotBlank()) {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()

                Handler(Looper.myLooper()!!).postDelayed({
                    binding.scanner.readNext()
                }, 1000)
            }
        }

        binding.flashBtn.setOnClickListener {
            binding.scanner.changeTorchState()
        }
    }

}