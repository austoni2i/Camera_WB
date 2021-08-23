package com.app.camerawb

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.camera2apipoc.CenterView
import com.app.camerawb.databinding.ActivityCamera2Binding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.controls.WhiteBalance
import kotlinx.android.synthetic.main.activity_camera2.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import java.io.File
import android.graphics.*
import android.media.Image
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import com.app.camera2apipoc.ImageUtils.processCropping
import com.app.camera2apipoc.ImageUtils.save
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.PictureResult
import org.wysaid.common.Common
import org.wysaid.nativePort.CGENativeLibrary
import java.io.ByteArrayOutputStream
import java.io.IOException


@RuntimePermissions
class MainActivity : AppCompatActivity(), CenterView.OnSwipeListener {
    private lateinit var binding: ActivityCamera2Binding
    private lateinit var colorTool: ColorTool
    var cameraView: CameraView? = null
    var dX = 0f
    var dY = 0f
    var patientId: String? = null
    var dialog: AlertDialog? = null
    var snackbar: Snackbar? = null

    //lateinit var gpuImage: GPUImage
    var ruleString = ""
    var isManualMode = false
    val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera2)
        binding.lifecycleOwner = this
        binding.vm = viewModel
        binding.activity = this
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        patientId = "123456"
        patientId ?: onBackPressed()

        initViewsWithPermissionCheck()
        setupGPUImage()
        setupGPUImagePlus()
    }

    @NeedsPermission(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun initViews() {
        AppPrefs.initPrefs(this)
        colorTool = ColorTool()
        cameraView = binding.cameraView
        cameraView?.setLifecycleOwner(this)
        cameraView?.useDeviceOrientation = false


        cameraView?.addCameraListener(object : CameraListener() {
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)

                result.toBitmap { bmp ->
                    val filterbmp = addFliter(bmp)
                    val bmpArray = bitmapToByteArray(filterbmp)

                    processCropping(bmpArray, result.size)?.let {
                        save(this@MainActivity, it) { uriString ->
                            uriString ?: return@save
                            val intent =
                                Intent(this@MainActivity, CameraPreviewActivity::class.java)
                            intent.putExtra("IMG_URI", uriString)
                            startActivity(intent)
                            overridePendingTransition(
                                android.R.anim.fade_in,
                                android.R.anim.fade_out
                            )

                        }

                    }
                }

            }
        })
        binding.centerView.setSwipeListener(this)
        if (!AppPrefs.isSettingDone) {
            Snackbar.make(
                binding.clRoot,
                "Initial-setup drag your camera view into center of the circle",
                Snackbar.LENGTH_LONG
            ).show()
            enableCalibrationMode()
        } else {
            AppPrefs.apply {
                binding.cameraView.x = cameraXadjustment.toFloat()
                binding.cameraView.y = cameraYadjustment.toFloat()
                binding.ivFilterView.x = cameraXadjustment.toFloat()
                binding.ivFilterView.y = cameraYadjustment.toFloat()
            }
        }

        binding.btnCalibrate.setOnClickListener {
            //takePicture()
            enableCalibrationMode(!(binding.centerView.isCalibrationMode))
        }

        binding.btnSaveCalibration.setOnClickListener {
            AppPrefs.apply {
                cameraXadjustment = binding.cameraView.x.toInt()
                cameraYadjustment = binding.cameraView.y.toInt()
                cameraXadjustment = binding.ivFilterView.x.toInt()
                cameraYadjustment = binding.ivFilterView.y.toInt()
                isSettingDone = true
            }
            enableCalibrationMode(false)
        }

        binding.btnCapture.setOnClickListener {
            getFileToSave()?.let {
                //camera2Helper?.takePicture(it, true)
                cameraView?.takePicture()
            }
        }

        btnWhitBalanceSettings?.setOnClickListener {
            showWBManualRGB(false)
            clWhiteBalanceContainer?.apply {
                if (visibility == View.VISIBLE) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                }
            }
        }

        btnOriginal?.setOnClickListener {
            resetManualFilters()
            ruleString = ""
        }

        btnParam1?.setOnClickListener {
            isManualMode = false
            resetManualFilters()
            ruleString = "@adjust lut param1.png"
        }

        btnParam2?.setOnClickListener {
            isManualMode = false
            resetManualFilters()
            ruleString = "@adjust lut param2.png"
        }

        btnParam3?.setOnClickListener {
            isManualMode = false
            resetManualFilters()
            ruleString = "@adjust lut param3.png"
        }

        btnParam4?.setOnClickListener {
            isManualMode = false
            resetManualFilters()
            ruleString = "@adjust lut param4.png"
        }


        btnManual?.setOnClickListener {
            isManualMode = true
            showManualControlPanel()
        }

        btnWarmFlurocet?.setOnClickListener {
            resetManualWhiteBalance()
            cameraView?.whiteBalance = WhiteBalance.WARM_FLUORESCENT
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT)
        }


        btnManualWhitBalance?.setOnClickListener {
            clWhiteBalanceContainer?.visibility = View.GONE
            seekBarWBGreen?.apply {
                if (visibility == View.VISIBLE) {
                    showWBManualRGB(false)
                } else {
                    showWBManualRGB()
                }
            }

        }

        btnManualClose?.setOnClickListener {
            showManualControlPanel(false)
            hideKeyboard()

        }

        resetManualWhiteBalance()

        switchAf?.setOnCheckedChangeListener { compoundButton, b ->

        }




        cameraView?.addFrameProcessor {
            if (it.dataClass === ByteArray::class.java) {
                val data: ByteArray = it.getData()

                /*OpenCV
                val out = ByteArrayOutputStream()
                val yuvImage = YuvImage(data, ImageFormat.NV21, it.size.width, it.size.height, null)
                yuvImage.compressToJpeg(Rect(0, 0, it.size.width, it.size.height), 100, out)
                val imageBytes: ByteArray = out.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val mat = Mat()
                Utils.bitmapToMat(bitmap, mat)

                /* Brightness increase beta 1.0 ... 100.0*/
                mat.convertTo(mat, -1, 1.0, -100.0)

                /* Contrast increase alpha 0.0 ... 2.5
                mat.convertTo(mat, -1, 2.0, 0.0)*/

                Utils.matToBitmap(mat, bitmap)
                runOnUiThread { ivFilterView.setImageBitmap(bitmap) }
                */
                if (isManualMode)
                    applyManualRule()
                //OpenGl
                val out = ByteArrayOutputStream()
                val yuvImage = YuvImage(data, ImageFormat.NV21, it.size.width, it.size.height, null)
                yuvImage.compressToJpeg(Rect(0, 0, it.size.width, it.size.height), 100, out)
                val imageBytes: ByteArray = out.toByteArray()
                val image = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                val bmp = addFliter(image)
                runOnUiThread {
                    binding.ivFilterView.setImageBitmap(bmp)
                }


            } else if (it.dataClass === Image::class.java) {
                val data: Image = it.getData()
                // Process android.media.Image...
            }
        }

        binding.seekBarRed.addOnChangeListener { slider, value, fromUser ->
            viewModel.red.value = value
        }
        binding.seekBarGreen.addOnChangeListener { slider, value, fromUser ->
            viewModel.green.value = value
        }
        binding.seekBarBlue.addOnChangeListener { slider, value, fromUser ->
            viewModel.blue.value = value
        }
        binding.seekBarCyan.addOnChangeListener { slider, value, fromUser ->
            viewModel.cyan.value = value
        }
        binding.seekBarMagenta.addOnChangeListener { slider, value, fromUser ->
            viewModel.magenta.value = value
        }
        binding.seekBarYellow.addOnChangeListener { slider, value, fromUser ->
            viewModel.yellow.value = value
        }
        binding.seekBarHue.addOnChangeListener { slider, value, fromUser ->
            viewModel.hue.value = value
        }
        binding.seekBarBrightness.addOnChangeListener { slider, value, fromUser ->
            viewModel.brt.value = value
        }
        binding.seekBarContrast.addOnChangeListener { slider, value, fromUser ->
            viewModel.contrast.value = value
        }


    }

    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0)
    }

    fun onApplied(view: TextView?, actionId: Int, event: KeyEvent?): Boolean {


        val value = view?.text.toString().toFloat()
        when (view) {
            binding.etRed -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.red)
            }
            binding.etGreen -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.green)
            }
            binding.etBlue -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.blue)
            }
            binding.etCyan -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.cyan)
            }
            binding.etMagenta -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.magenta)
            }
            binding.etYellow -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.yellow)
            }
            binding.etHue -> {
                return applyEtValues(view, value, hueMin, hueMax, viewModel.hue)
            }
            binding.etWB -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.wb)
            }
            binding.etBrt -> {
                return applyEtValues(view, value, satMin, satMax, viewModel.brt)
            }
            binding.etCon -> {
                return applyEtValues(view, value, contrastMin, contrastMax, viewModel.contrast)
            }
        }
        return false
    }

    fun applyEtValues(
        view: TextView,
        value: Float,
        min: Float,
        max: Float,
        param: MutableLiveData<Float>
    ): Boolean {
        if (value in min..max) {
            param.value = view.text.toString().toFloat()
            return false
        } else {
            view.error = "Value should be with $min to $max"
            return true
        }
    }

    fun fabAdjustValue(view: View){
        val speed2x = 0.1f
        val speed1x = 0.01f
        when(view){
            binding.fabRAdd -> {
                val result = viewModel.red.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                viewModel.red.value = result
            }
            binding.fabRMinus -> {
                val result = viewModel.red.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                viewModel.red.value = result
            }
            binding.fabGAdd -> {
                val result = viewModel.green.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                viewModel.green.value = result
            }
            binding.fabGMinus -> {
                val result = viewModel.green.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.green.value = result
            }
            binding.fabBAdd -> {
                val result = viewModel.blue.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.blue.value = result
            }
            binding.fabBMinus -> {
                val result = viewModel.blue.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.blue.value = result
            }
            binding.fabCAdd -> {
                val result = viewModel.cyan.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.cyan.value = result
            }
            binding.fabCMinus -> {
                val result = viewModel.cyan.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.cyan.value = result
            }
            binding.fabMagAdd -> {
                val result = viewModel.magenta.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.magenta.value = result
            }
            binding.fabMagMinus -> {
                val result = viewModel.magenta.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.magenta.value = result
            }
            binding.fabYellowAdd -> {
                val result = viewModel.yellow.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.yellow.value = result
            }
            binding.fabYellowMinus -> {
                val result = viewModel.yellow.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.yellow.value = result
            }
            binding.fabHueAdd -> {
                val result = viewModel.hue.value?.plus(speed2x)?:return
                if (result in hueMin..hueMax)
                    viewModel.hue.value = result
            }
            binding.fabHueMinus -> {
                val result = viewModel.hue.value?.minus(speed2x)?:return
                if (result in hueMin..hueMax)
                    viewModel.hue.value = result
            }
            binding.fabWBAdd -> {
                val result = viewModel.wb.value?.plus(speed1x)?:return
                if (result in satMin..satMax)
                    viewModel.wb.value = result
            }
            binding.fabWBMinus -> {
                val result = viewModel.wb.value?.minus(speed1x)?:return
                if (result in satMin..satMax)
                    viewModel.wb.value = result
            }
            binding.fabBrtAdd -> {
                val result = viewModel.brt.value?.plus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.brt.value = result
            }
            binding.fabBrtMinus -> {
                val result = viewModel.brt.value?.minus(speed2x)?:return
                if (result in satMin..satMax)
                    viewModel.brt.value = result
            }
            binding.fabConAdd -> {
                val result = viewModel.contrast.value?.plus(speed2x)?:return
                if (result in contrastMin..contrastMax)
                    viewModel.contrast.value = result
            }
            binding.fabConMinus -> {
                val result = viewModel.contrast.value?.minus(speed2x)?:return
                if (result in contrastMin..contrastMax)
                    viewModel.contrast.value = result
            }

        }
    }

    fun getTwoDecimalFloat(float: Float): String {
        return String.format("%.2f", float)
    }


    private fun resetManualFilters() {
        seekBarRed.value = redDefault
        seekBarGreen.value = greenDefault
        seekBarBlue.value = blueDefault
        seekBarCyan.value = cyanDefault
        seekBarMagenta.value = magentaDefault
        seekBarYellow.value = yellowDefault
        seekBarHue.value = hueDefault
        seekBarWB.value = wbDefault
        seekBarBrightness.value = brtDefault
        seekBarContrast.value = conDefault
    }

    private fun applyManualRule() {
        val red = seekBarRed.value
        val green = seekBarGreen.value
        val blue = seekBarBlue.value
        val cyan = seekBarCyan.value
        val magenta = seekBarMagenta.value
        val yellow = seekBarYellow.value
        val hue = seekBarHue.value
        val wb = seekBarWB.value
        val brightness = seekBarBrightness.value
        val contrast = seekBarContrast.value
        ruleString =
            "@adjust hsv $red $green $blue $magenta $yellow $cyan @adjust hue $hue @adjust whitebalance $wb 1 @adjust brightness $brightness @adjust contrast $contrast "
    }

    private fun showManualControlPanel(show: Boolean = true) {
        val visibility = if (show) View.VISIBLE else View.GONE
        clManualControlContainer.visibility = visibility
        btnManualClose.visibility = visibility
        if (show) clWhiteBalanceContainer.visibility = View.GONE
    }

    private fun bitmapToByteArray(bmp: Bitmap?): ByteArray {
        val stream = ByteArrayOutputStream()
        bmp?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        bmp?.recycle()
        return byteArray
    }

    private fun setupGPUImage() {
        /*  gpuImage = GPUImage(this)
          val istr = assets.open("lookupbluesat.png")
          val lutbitmap = BitmapFactory.decodeStream(istr)
          istr.close()
          val filter = GPUImageLookupFilter()
          filter.bitmap = lutbitmap
          gpuImage.setFilter(filter)*/
    }

    private fun setupGPUImagePlus() {
        CGENativeLibrary.setLoadImageCallback(object : CGENativeLibrary.LoadImageCallback {
            override fun loadImage(name: String?, arg: Any?): Bitmap? {
                val am = assets
                val istr = try {
                    am.open(name!!)
                } catch (e: IOException) {
                    Log.e(Common.LOG_TAG, "Can not open file $name")
                    return null
                }

                return BitmapFactory.decodeStream(istr)
            }

            override fun loadImageOK(bmp: Bitmap?, arg: Any?) {
                bmp!!.recycle()
            }
        }, null)
    }

    private fun addFliter(image: Bitmap?): Bitmap? {
        image ?: return null


        /*gpuImage.setImage(image)
        return gpuImage.bitmapWithFilterApplied*/
        /*
        * working
        *
        return CGENativeLibrary.filterImage_MultipleEffects(image, ruleString, 1.0f)

         */

        return CGENativeLibrary.filterImage_MultipleEffects(image, ruleString, 1.0f)
    }


    fun showWBManualRGB(show: Boolean = true) {
        val visibility = if (show) View.VISIBLE else View.GONE
        seekBarWBRed.visibility = visibility
        seekBarWBGreen.visibility = visibility
        seekBarWBBlue.visibility = visibility
    }

    private fun drawDummyCircleCenter() {
        centerView?.post {
            val firstLineY = centerView.centerY - centerView.radius
            addDummyLine(null, firstLineY)
            val secondLineY = centerView.centerY + centerView.radius
            addDummyLine(null, secondLineY)

            val firstLineX = centerView.centerX - centerView.radius
            addDummyLine(firstLineX, null)
            val secondLineX = centerView.centerX + centerView.radius
            addDummyLine(secondLineX, null)

            //camera2Helper?.setCropParams(45f, 435f, 205f, 595f)
        }
    }

    private fun resetManualWhiteBalance() {
        //seekBarWB?.setProgress(5000f)
    }

    private fun enableCalibrationMode(enable: Boolean? = true) {
        binding.centerView?.isCalibrationMode = enable ?: true

        binding.centerView?.isCalibrationMode?.let { isEnabled ->
            if (isEnabled) {
                binding.btnCapture?.hide(object :
                    FloatingActionButton.OnVisibilityChangedListener() {
                    override fun onHidden(fab: FloatingActionButton?) {
                        super.onHidden(fab)
                        binding.btnCalibrate?.hide()
                        binding.btnSaveCalibration.show()
                    }
                })
            } else {
                binding.btnSaveCalibration?.hide(object :
                    FloatingActionButton.OnVisibilityChangedListener() {
                    override fun onHidden(fab: FloatingActionButton?) {
                        super.onHidden(fab)
                        binding.btnCapture?.show()
                        binding.btnCalibrate?.show()
                    }
                })
            }
        }

    }


    override fun onResume() {
        super.onResume()
        //camera2Helper?.onResume()
    }

    override fun onPause() {
        //camera2Helper?.onPause()
        super.onPause()
    }

    override fun onSwipe(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                /*dX = binding.cameraView.getX() - event.rawX
                dY = binding.cameraView.getY() - event.rawY*/
                dX = binding.ivFilterView.getX() - event.rawX
                dY = binding.ivFilterView.getY() - event.rawY
            }
            MotionEvent.ACTION_MOVE -> binding.ivFilterView.animate()
                .x(event.rawX + dX)
                .y(event.rawY + dY)
                .setDuration(0)
                .start()
        }
    }


    fun getFileToSave(): File? {
        patientId ?: onBackPressed()
        try {
            val cw = ContextWrapper(this)
            val directory = cw.getDir(patientId, Context.MODE_PRIVATE)
            val file = File(directory, "Demo.jpeg")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            onBackPressed()
            return null
        }
    }


    override fun onDestroy() {
        dialog?.dismiss()
        super.onDestroy()
    }

    fun addDummyLine(xTarget: Float?, yTarget: Float?) {
        val line = View(this)
        line.apply {
            if (xTarget == null) {
                layoutParams =
                    ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, 5)
            } else {
                layoutParams =
                    ConstraintLayout.LayoutParams(5, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            }
            elevation = 5f
            setBackgroundColor(Color.RED)

            val location = IntArray(2)
            xTarget?.let {
                x = xTarget
                line.post {
                    getLocationInWindow(location)
                    location.forEach {
                        Log.d("syscoor", "X: ${it} ")
                    }
                }

            }

            yTarget?.let {
                y = yTarget
                line.post {
                    getLocationInWindow(location)
                    location.forEach {
                        Log.d("syscoor", "Y: ${it} ")
                    }
                }

            }

        }
        clRoot.addView(line)
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    fun onCameraDenied() {
        Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
    }

    @OnNeverAskAgain(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onCameraNeverAskAgain() {
        Toast.makeText(this, "Some Permission is disabled permanently", Toast.LENGTH_SHORT).show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onBackPressed() {

        if (clWhiteBalanceContainer?.visibility == View.VISIBLE || clManualControlContainer.visibility == View.VISIBLE) {
            clWhiteBalanceContainer?.visibility = View.GONE
            showManualControlPanel(false)
        } else {
            super.onBackPressed()
        }

    }

    fun isKeyboardVisible(callback: (visible: Boolean)->Unit){

        binding.clRoot.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener{
            override fun onGlobalLayout() {
                val r = Rect()
                //r will be populated with the coordinates of your view that area still visible.
                binding.clRoot.getWindowVisibleDisplayFrame(r);

                val heightDiff = binding.clRoot.rootView.height - r.height();
                if (heightDiff > 0.25*binding.clRoot.rootView.height) { // if more than 25% of the screen, its probably a keyboard...
                    callback.invoke(true)
                }else
                    callback.invoke(false)
            }
        });
    }

    companion object{
        const val satMax = 1f
        const val satMin = -1f
        const val hueMin = 0f
        const val hueMax = 6.2f
        const val contrastMin = 0f
        const val contrastMax = 5f

        const val redDefault = 0.0f
        const val greenDefault = 0.0f
        const val blueDefault = 0.0f
        const val cyanDefault = 0.0f
        const val magentaDefault = 0.0f
        const val yellowDefault = 0.0f
        const val hueDefault = 0.0f
        const val wbDefault = 0.0f
        const val brtDefault = 0.0f
        const val conDefault = 1.0f
    }


}