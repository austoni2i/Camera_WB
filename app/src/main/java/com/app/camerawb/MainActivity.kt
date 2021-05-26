package com.app.camerawb

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.app.camera2apipoc.CenterView
import com.app.camera2apipoc.ImageUtils.processCropping
import com.app.camera2apipoc.ImageUtils.save
import com.app.camerawb.databinding.ActivityCamera2Binding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.controls.WhiteBalance
import kotlinx.android.synthetic.main.activity_camera2.*
import permissions.dispatcher.NeedsPermission
import permissions.dispatcher.OnNeverAskAgain
import permissions.dispatcher.OnPermissionDenied
import permissions.dispatcher.RuntimePermissions
import java.io.File

@RuntimePermissions
class MainActivity : AppCompatActivity(), CenterView.OnSwipeListener {
    private lateinit var binding: ActivityCamera2Binding
    var cameraView: CameraView? = null
    var dX = 0f
    var dY = 0f
    var patientId: String? = null
    var dialog: AlertDialog? = null
    var snackbar: Snackbar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamera2Binding.inflate(layoutInflater)
        val view = binding.root
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(view)
        patientId = "123456"
        patientId ?: onBackPressed()

        initViewsWithPermissionCheck()
    }

    @NeedsPermission(Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun initViews() {
        AppPrefs.initPrefs(this)
        cameraView = binding.cameraView
        cameraView?.setLifecycleOwner(this)
        cameraView?.useDeviceOrientation = false

        cameraView?.addCameraListener(object : CameraListener(){
            override fun onPictureTaken(result: PictureResult) {
                super.onPictureTaken(result)
                /*save(this@MainActivity, result.data){uriString->
                    val intent = Intent(this@MainActivity, CameraPreviewActivity::class.java)
                    intent.putExtra("IMG_URI", uriString.toString())
                    startActivity(intent)
                }
                return*/
                processCropping(result.data, result.size)?.let {
                    save(this@MainActivity, it){uriString->
                        val intent = Intent(this@MainActivity, CameraPreviewActivity::class.java)
                        intent.putExtra("IMG_URI", uriString.toString())
                        startActivity(intent)
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

        btnAuto?.setOnClickListener {
            resetManualWhiteBalance()
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_AUTO)
            cameraView?.whiteBalance = WhiteBalance.AUTO
        }

        btnCloudy?.setOnClickListener {
            resetManualWhiteBalance()
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT)
            cameraView?.whiteBalance = WhiteBalance.CLOUDY
        }

        btnDaylight?.setOnClickListener {
            resetManualWhiteBalance()
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT)
            cameraView?.whiteBalance = WhiteBalance.DAYLIGHT
        }

        btnFlurocent?.setOnClickListener {
            resetManualWhiteBalance()
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT)
            cameraView?.whiteBalance = WhiteBalance.FLUORESCENT
        }

        btnIncandacent?.setOnClickListener {
            resetManualWhiteBalance()
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT)
            cameraView?.whiteBalance = WhiteBalance.INCANDESCENT
        }

        btnShade?.setOnClickListener {
            resetManualWhiteBalance()
            cameraView?.whiteBalance = WhiteBalance.SHADE
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_SHADE)
        }

        btnTwlight?.setOnClickListener {
            resetManualWhiteBalance()
            cameraView?.whiteBalance = WhiteBalance.TWILIGHT
            //camera2Helper?.setWhiteBalance(CameraMetadata.CONTROL_AWB_MODE_TWILIGHT)
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
            /*seekBarWB?.apply {
                if (visibility == View.VISIBLE) {
                    visibility = View.GONE
                } else {
                    visibility = View.VISIBLE
                }
            }*/
        }

        resetManualWhiteBalance()


       /* seekBarWBRed?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                camera2Helper?.setManualWhiteBalanceRGB(
                    seekBarWBRed?.value,
                    seekBarWBGreen.value,
                    seekBarWBBlue.value
                )
            }

            override fun onStopTrackingTouch(slider: Slider) {
                camera2Helper?.setManualWhiteBalanceRGB(
                    seekBarWBRed?.value,
                    seekBarWBGreen.value,
                    seekBarWBBlue.value
                )
            }
        })


        seekBarWBGreen?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                camera2Helper?.setManualWhiteBalanceRGB(
                    seekBarWBRed?.value,
                    seekBarWBGreen.value,
                    seekBarWBBlue.value
                )
            }

            override fun onStopTrackingTouch(slider: Slider) {
                camera2Helper?.setManualWhiteBalanceRGB(
                    seekBarWBRed?.value,
                    seekBarWBGreen.value,
                    seekBarWBBlue.value
                )
            }
        })


        seekBarWBBlue?.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                camera2Helper?.setManualWhiteBalanceRGB(
                    seekBarWBRed?.value,
                    seekBarWBGreen.value,
                    seekBarWBBlue.value
                )
            }

            override fun onStopTrackingTouch(slider: Slider) {
                camera2Helper?.setManualWhiteBalanceRGB(
                    seekBarWBRed?.value,
                    seekBarWBGreen.value,
                    seekBarWBBlue.value
                )
            }
        })*/



        switchAf?.setOnCheckedChangeListener { compoundButton, b ->

        }

        //drawDummyCircleCenter()

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
                dX = binding.cameraView.getX() - event.rawX
                dY = binding.cameraView.getY() - event.rawY
            }
            MotionEvent.ACTION_MOVE -> binding.cameraView.animate()
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

    @OnNeverAskAgain(Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE)
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

        if(clWhiteBalanceContainer?.visibility == View.VISIBLE){
            clWhiteBalanceContainer?.visibility  = View.GONE
        }else{
            super.onBackPressed()
        }

    }


}