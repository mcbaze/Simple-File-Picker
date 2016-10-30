package com.simplemobiletools.filepicker.dialogs

import android.app.Activity
import android.os.Build
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import com.simplemobiletools.filepicker.R
import com.simplemobiletools.filepicker.extensions.getInternalStoragePath
import com.simplemobiletools.filepicker.extensions.getSDCardPath

class StoragePickerDialog(val activity: Activity, val basePath: String, val listener: OnStoragePickerListener) {
    var mDialog: AlertDialog?
    val mContext = activity

    init {
        val inflater = LayoutInflater.from(mContext)
        val resources = mContext.resources
        val layoutParams = RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val radioGroup = inflater.inflate(R.layout.smtfp_radio_group, null) as RadioGroup

        val internalButton = inflater.inflate(R.layout.smtfp_radio_button, null) as RadioButton
        internalButton.apply {
            text = resources.getString(R.string.smtfp_internal)
            isChecked = basePath == context.getInternalStoragePath()
            setOnClickListener { internalPicked() }
        }
        radioGroup.addView(internalButton, layoutParams)

        if (isSDCardAvailable()) {
            val sdButton = inflater.inflate(R.layout.smtfp_radio_button, null) as RadioButton
            sdButton.apply {
                text = resources.getString(R.string.smtfp_sd_card)
                isChecked = basePath == context.getSDCardPath()
                setOnClickListener { sdPicked() }
            }
            radioGroup.addView(sdButton, layoutParams)
        }

        val rootButton = inflater.inflate(R.layout.smtfp_radio_button, null) as RadioButton
        rootButton.apply {
            text = resources.getString(R.string.smtfp_root)
            isChecked = basePath == "/"
            setOnClickListener { rootPicked() }
        }
        radioGroup.addView(rootButton, layoutParams)

        mDialog = AlertDialog.Builder(mContext)
                .setTitle(mContext.resources.getString(R.string.smtfp_select_storage))
                .setView(radioGroup)
                .create()

        mDialog?.show()
    }

    private fun internalPicked() {
        mDialog?.dismiss()
        listener.onPick(mContext.getInternalStoragePath())
    }

    private fun sdPicked() {
        mDialog?.dismiss()
        listener.onPick(mContext.getSDCardPath())
    }

    private fun rootPicked() {
        mDialog?.dismiss()
        listener.onPick("/")
    }

    private fun isSDCardAvailable() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    interface OnStoragePickerListener {
        fun onPick(pickedPath: String)
    }
}
