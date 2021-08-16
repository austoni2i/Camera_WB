package com.app.camerawb

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.WindowManager
import androidx.core.net.toUri
import com.app.camera2apipoc.AppUtils
import com.app.camerawb.databinding.ActivityCameraPreviewBinding
import kotlinx.android.synthetic.main.activity_camera_preview.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.zomato.photofilters.imageprocessors.Filter
import org.wysaid.myUtils.ImageUtil

import org.wysaid.nativePort.CGENativeLibrary
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter

import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter


class CameraPreviewActivity : AppCompatActivity() {

    lateinit var binding: ActivityCameraPreviewBinding
    var imgUri: Uri?=null

    init {
        System.loadLibrary("NativeImageProcessor")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        AppUtils.hideSystemUI(this, true)
        window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        imgUri = intent?.getStringExtra("IMG_URI")?.toUri()

        ivPreview?.setImageURI(imgUri)
        //processImg(imgUri)
        //processImgZomato(imgUri)
    }

   /* private fun processImgZomato(imgUri: Uri?) {
        val myFilter = Filter()
        myFilter.addSubFilter(BrightnessSubFilter(1.5))
        myFilter.addSubFilter(ContrastSubFilter(1.1f))
        val outputImage = myFilter.processFilter(getBitmapImg(imgUri!!))
        ivPreview?.setImageBitmap(outputImage)
    }*/

    private fun processImg(imgUri: Uri?) {
        imgUri?:return
        val srcImage = getBitmapImg(imgUri)


        val ruleString = "@adjust brightness 1"
        val dstImage = CGENativeLibrary.filterImage_MultipleEffects(srcImage, ruleString, 1.0f)
        ImageUtil.saveBitmap(dstImage)
        ivPreview?.setImageBitmap(dstImage)
    }

    private fun getBitmapImg(imgUri: Uri): Bitmap {
        val uri = Uri.parse("file://$imgUri")
        return  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }.copy(Bitmap.Config.ARGB_8888, true)
    }
}