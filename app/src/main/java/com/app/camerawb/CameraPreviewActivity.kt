package com.app.camerawb

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import androidx.core.net.toUri
import com.app.camera2apipoc.AppUtils
import kotlinx.android.synthetic.main.activity_camera_preview.*

class CameraPreviewActivity : AppCompatActivity() {

    var imgUri: Uri?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        AppUtils.hideSystemUI(this, true)
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        imgUri = intent?.getStringExtra("IMG_URI")?.toUri()

        ivPreview?.setImageURI(imgUri)
    }
}