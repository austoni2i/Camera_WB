package com.app.camera2apipoc

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object AppUtils {

    fun showAlertDialog(
            context: Context?,
            title: String = "",
            msg: String = "",
            noClick: ((dialog: DialogInterface) -> Unit)? = null,
            yesClick: ((dialog: DialogInterface) -> Unit)? = null

    ) {
        context?:return
        MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(msg)

                .setNegativeButton("No") { dialog, which ->
                    noClick?.invoke(dialog)
                }
                .setPositiveButton("Yes") { dialog, which ->
                    yesClick?.invoke(dialog)
                }
                .show()
    }

    fun showAlertDialog(
            context: Context?,
            title: String = "",
            msg: String = "",
            okClick: ((dialog: DialogInterface) -> Unit)? = null

    ): AlertDialog? {
        context?:return null

        if (context is Activity){
            if (context.isFinishing){
                return null
            }
        }
        val dialog:AlertDialog = MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("Ok") { dialog, which ->
                    okClick?.invoke(dialog)
                }.create()
        dialog.show()
        return dialog

    }

    fun hideSystemUI(activity: Activity, showStatusBar: Boolean = true) {

        if (!showStatusBar) {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
               activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
            } else {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                )
            }
        }
    }

}