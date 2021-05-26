package com.app.camerawb

import android.content.Context
import android.content.SharedPreferences

object AppPrefs {

    const val IS_CAMERA_SETTING_DONE = "IS_CAMERA_SETTING_DONE"
    const val CAMERA_X_ADJUSTMENT = "CAMERA_X_ADJUSTMENT"
    const val CAMERA_Y_ADJUSTMENT = "CAMERA_Y_ADJUSTMENT"

    var prefs: SharedPreferences?=null

    fun initPrefs(context: Context?){
        prefs = context?.getSharedPreferences("camera2api", Context.MODE_PRIVATE)
    }

    var isSettingDone: Boolean
        get() = prefs?.getBoolean(IS_CAMERA_SETTING_DONE, false)?:false
        set(value) = prefs?.edit()?.putBoolean(IS_CAMERA_SETTING_DONE, value)?.apply()!!

    var cameraXadjustment: Int
        get() = prefs?.getInt(CAMERA_X_ADJUSTMENT, 0)?:0
        set(value) = prefs?.edit()?.putInt(CAMERA_X_ADJUSTMENT, value)?.apply()!!

    var cameraYadjustment: Int
        get() = prefs?.getInt(CAMERA_Y_ADJUSTMENT, 0)?:0
        set(value) = prefs?.edit()?.putInt(CAMERA_Y_ADJUSTMENT, value)?.apply()!!

}