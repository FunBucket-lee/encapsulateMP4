package com.qing.encapsulatemp4

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import com.qing.encapsulatemp4.databinding.MainActivityLayoutBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: MainActivityLayoutBinding

    companion object {
        private const val REQUEST_EXTERNAL_STORAGE = 1
        private val PERMISSIONS_STORAGE = arrayOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )

        @JvmStatic
        private fun verifyStoragePermissions(activity: Activity) {
            try {//检查是否有读写权限
                val permission = ActivityCompat.checkSelfPermission(
                    activity,
                    "android.permission.WRITE_EXTERNAL_STORAGE"
                )
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                        activity,
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainActivityLayoutBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        verifyStoragePermissions(this)
        binding.apply {
            //分离音频
            btnExtractorAudio.setOnClickListener {
                MediaUtils.extractorAudio(this@MainActivity)
            }
            //分离视频
            btnExtractorVideo.setOnClickListener {
                MediaUtils.extractorVideo(this@MainActivity)
            }
            //合成音视频
            btnMuxerVideoaudio.setOnClickListener {
                MediaUtils.muxerVideoAudio(this@MainActivity)
            }
        }
    }
}
