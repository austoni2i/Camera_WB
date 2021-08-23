package com.app.camerawb

import android.view.inputmethod.EditorInfo
import android.widget.EditText

fun EditText.onApply(callback: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            callback.invoke()
            true
        }
        false
    }
}
