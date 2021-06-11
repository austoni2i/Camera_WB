package com.app.camera2apipoc

import RefreshGallery
import android.content.Context
import android.content.ContextWrapper
import android.graphics.*
import android.media.Image
import android.os.Environment
import android.util.Log
import com.app.camerawb.AppPrefs
import com.otaliastudios.cameraview.size.Size
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


object ImageUtils {

    fun rotateJPEG(jpeg: ByteArray, quality: Int, degrees: Int): ByteArray? {
        val bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        val rotated: Bitmap
        val m = Matrix()
        m.postRotate(degrees.toFloat())
        rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
        val baos = ByteArrayOutputStream()
        rotated.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val result: ByteArray = baos.toByteArray()
        bitmap.recycle()
        rotated.recycle()
        return getCroppedBitmap(result)
    }

    fun convertToJPEG(image: Image): ByteArray? {
        val jpeg: ByteArray
        if (image.getFormat() == ImageFormat.JPEG) {
            jpeg = readJPEG(image)?:return null
        } else if (image.getFormat() == ImageFormat.YUV_420_888) {
            jpeg = NV21toJPEG(YUV420toNV21(image)?:return null, image.getWidth(), image.getHeight(), 100)?:return null
        } else {
            throw RuntimeException("Unsupported format: " + image.getFormat())
        }
        return jpeg
    }

    private fun readJPEG(jpegImage: Image): ByteArray? {
        val buffer: ByteBuffer = jpegImage.planes[0].buffer
        val data = ByteArray(buffer.remaining())
        buffer.get(data, 0, data.size)
        return data
    }

    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int, quality: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), quality, out)
        return out.toByteArray()
    }

    private fun YUV420toNV21(image: Image): ByteArray? {
        val crop = image.cropRect
        val format = image.format
        val width = crop.width()
        val height = crop.height()
        val planes = image.planes
        val data = ByteArray(width * height * ImageFormat.getBitsPerPixel(format) / 8)
        val rowData = ByteArray(planes[0].rowStride)
        var channelOffset = 0
        var outputStride = 1
        for (i in planes.indices) {
            when (i) {
                0 -> {
                    channelOffset = 0
                    outputStride = 1
                }
                1 -> {
                    channelOffset = width * height + 1
                    outputStride = 2
                }
                2 -> {
                    channelOffset = width * height
                    outputStride = 2
                }
            }
            val buffer = planes[i].buffer
            val rowStride = planes[i].rowStride
            val pixelStride = planes[i].pixelStride
            val shift = if (i == 0) 0 else 1
            val w = width shr shift
            val h = height shr shift
            buffer.position(rowStride * (crop.top shr shift) + pixelStride * (crop.left shr shift))
            for (row in 0 until h) {
                var length: Int
                if (pixelStride == 1 && outputStride == 1) {
                    length = w
                    buffer[data, channelOffset, length]
                    channelOffset += length
                } else {
                    length = (w - 1) * pixelStride + 1
                    buffer[rowData, 0, length]
                    for (col in 0 until w) {
                        data[channelOffset] = rowData[col * pixelStride]
                        channelOffset += outputStride
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length)
                }
            }
        }
        return data
    }

    fun getCroppedBitmap(byteArray: ByteArray): ByteArray {

        val widthOffset = AppPrefs.cameraXadjustment
        val heightOffset = AppPrefs.cameraYadjustment
        val opt = BitmapFactory.Options()
        opt.inMutable = true
        val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, opt)

        val widthBoost = 1280f/800f
        val heightBoost = 720f/480f
        val cx = (widthOffset*widthBoost).toFloat() // 4.08 = bmp.width(3264)/textureview.width(800) after merasurement by system
        val cy = (heightOffset*heightBoost).toFloat() // 5.19 = bmp.height(2488)/textureview.height(480) after merasurement by system
        val rect = Rect(0, 0, bmp.width, bmp.height)
        val radius = 195f*heightBoost // 750f = centerview circle radius* (bmp.height(2488)/textureview.height(480)) after merasurement by system

        val output = Bitmap.createBitmap(
                bmp.width,
                bmp.height, Bitmap.Config.ARGB_8888,
        )

        val canvasBackground = Canvas(output)
        val canvasImage = Canvas(bmp)
        canvasImage.translate(cx, cy)
        canvasImage.drawBitmap(bmp, rect, rect, Paint())

        val color = -0xbdbdbe
        val paint = Paint()

        paint.isAntiAlias = true
        canvasBackground.drawARGB(0, 0, 0, 0)
        paint.color = color

        canvasBackground.drawCircle(
                (bmp.width / 2).toFloat(), (bmp.height / 2).toFloat(), radius, paint
        )


        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)

        canvasBackground.drawBitmap(bmp, rect, rect, paint)


        val stream = ByteArrayOutputStream()
        output.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val opByteArray = stream.toByteArray()
        output.recycle()
        return opByteArray
    }

    fun processCropping(bytes: ByteArray?, size: Size):ByteArray? {
        bytes?:return null
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val radius = 173f
        var width = size.width/4
        var height = size.height/4
        var magicX = AppPrefs.cameraXadjustment/9
        var magicY = AppPrefs.cameraYadjustment/5
        val offset = Point(AppPrefs.cameraXadjustment-magicX, AppPrefs.cameraYadjustment-magicY)
        val matrix = Matrix()
        if (size.width<size.height){
            Log.d("processCropping", "rotated:  ${size.width}X${size.height}")
            matrix.postRotate(90f)
             width = size.height/4
             height = size.width/4
        }else{
            Log.d("processCropping", "rotated:  ${size.width}X${size.height}")
        }


        val rotatedBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, true
        )

        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width, height, true)

        val ampBitmap = Bitmap.createBitmap(
            scaledBitmap.width*2,
            scaledBitmap.height*2, Bitmap.Config.ARGB_8888,
        )
        val newCanvas = Canvas(ampBitmap)
        newCanvas.drawARGB(0, 0, 0, 0)
        newCanvas.translate(scaledBitmap.width/2f,scaledBitmap.height/2f)
        val rect2 = Rect(0, 0, scaledBitmap.width, scaledBitmap.height)
        newCanvas.drawBitmap(scaledBitmap,rect2, rect2, Paint())


        val croppedBitmap = Bitmap.createBitmap(
            ampBitmap.width,
            ampBitmap.height, Bitmap.Config.ARGB_8888,
        )
        val canvasBackground = Canvas(croppedBitmap)
        val color = -0xbdbdbe
        val paint = Paint()
        paint.isAntiAlias = true
        canvasBackground.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvasBackground.drawCircle(
            (ampBitmap.width / 2).toFloat(), (ampBitmap.height / 2).toFloat(), radius, paint
        )
        canvasBackground.translate(offset.x.toFloat(), offset.y.toFloat())
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val rect = Rect(0, 0, ampBitmap.width, ampBitmap.height)
        canvasBackground.drawBitmap(ampBitmap, rect, rect, paint)

        val finalCut = Bitmap.createBitmap(croppedBitmap, scaledBitmap.width/2,scaledBitmap.height/2,width, height)

        val stream = ByteArrayOutputStream()
        finalCut.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray = stream.toByteArray()
        finalCut.recycle()


        return byteArray
    }

    @Throws(IOException::class)
    fun save(context: Context, bytes: ByteArray, onSuccess: ((fileUri:String?)->Unit)?=null) {
        var output: OutputStream? = null
        try {
            getFileToSave(context)?.let {rawFile->
                output = FileOutputStream(rawFile)
                output?.write(bytes)
                onSuccess?.invoke(rawFile.absoluteFile.absolutePath.toString())
                refreshGalleryOnOlderDevice(context, rawFile)
            }
        } finally {
            output?.close()
        }
    }

    private fun refreshGalleryOnOlderDevice(context: Context, rawFile: File) {
        RefreshGallery(context, rawFile)
    }

    fun getFileToSave(context: Context): File? {
        try {
            val cw = ContextWrapper(context)
            //val directory = cw.getDir("PAT_0001", Context.MODE_PRIVATE)
            //val directory = cw.getExternalFilesDir("PAT_0001")
            val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM+"/CameraWB")

            if (!directory.exists() || !directory.isDirectory()){
                directory.mkdir()
            }
            val currentDate =
                SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.getDefault()).format(Date())
            val file = File(directory, "SHRUTI_$currentDate.jpg")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}