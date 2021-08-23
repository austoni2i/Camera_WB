package com.app.camerawb

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.app.camerawb.MainActivity.Companion.blueDefault
import com.app.camerawb.MainActivity.Companion.brtDefault
import com.app.camerawb.MainActivity.Companion.conDefault
import com.app.camerawb.MainActivity.Companion.cyanDefault
import com.app.camerawb.MainActivity.Companion.greenDefault
import com.app.camerawb.MainActivity.Companion.hueDefault
import com.app.camerawb.MainActivity.Companion.magentaDefault
import com.app.camerawb.MainActivity.Companion.redDefault
import com.app.camerawb.MainActivity.Companion.wbDefault
import com.app.camerawb.MainActivity.Companion.yellowDefault
import com.google.android.material.slider.Slider

class MainViewModel: ViewModel() {


    val red by lazy { MutableLiveData<Float>().also {it.value = redDefault} }
    val green by lazy { MutableLiveData<Float>().also {it.value = greenDefault} }
    val blue by lazy { MutableLiveData<Float>().also {it.value = blueDefault} }
    val cyan by lazy { MutableLiveData<Float>().also {it.value = cyanDefault} }
    val magenta by lazy { MutableLiveData<Float>().also {it.value = magentaDefault} }
    val yellow by lazy { MutableLiveData<Float>().also {it.value = yellowDefault} }
    val hue by lazy { MutableLiveData<Float>().also {it.value = hueDefault} }
    val wb by lazy { MutableLiveData<Float>().also {it.value = wbDefault} }
    val brt by lazy { MutableLiveData<Float>().also {it.value = brtDefault} }
    val contrast by lazy { MutableLiveData<Float>().also {it.value = conDefault} }



}