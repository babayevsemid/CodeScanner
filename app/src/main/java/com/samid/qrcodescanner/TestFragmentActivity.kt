package com.samid.qrcodescanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.samid.qrcodescanner.databinding.ActivityTestFragmentBinding

class TestFragmentActivity : AppCompatActivity() {
    private val binding by lazy {
         ActivityTestFragmentBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val transition=supportFragmentManager.beginTransaction()
        transition.replace(R.id.layout,BlankFragment())
        transition.commit()
    }
}