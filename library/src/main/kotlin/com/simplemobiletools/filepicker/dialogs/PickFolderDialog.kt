package com.simplemobiletools.filepicker.dialogs

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.view.View
import com.simplemobiletools.filepicker.R
import com.simplemobiletools.filepicker.adapters.ItemsAdapter
import com.simplemobiletools.filepicker.extensions.getFilenameFromPath
import com.simplemobiletools.filepicker.models.FileDirItem
import com.simplemobiletools.filepicker.views.Breadcrumbs
import kotlinx.android.synthetic.main.smtfp_directory_picker.view.*
import java.io.File
import java.util.*
import kotlin.comparisons.compareBy

class PickFolderDialog : DialogFragment(), Breadcrumbs.BreadcrumbsListener {

    companion object {
        lateinit var mPath: String
        var mFirstUpdate: Boolean = true
        var mShowHidden: Boolean = false
        var mShowFullPath: Boolean = false

        fun newInstance(path: String, showHidden: Boolean, showFullPath: Boolean): PickFolderDialog {
            mPath = path
            mFirstUpdate = true
            mShowHidden = showHidden
            mShowFullPath = showFullPath
            return PickFolderDialog()
        }
    }

    lateinit var dialog: View
    var requestCode = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog = activity.layoutInflater.inflate(R.layout.smtfp_directory_picker, null)
        requestCode = targetRequestCode

        updateItems()
        setupBreadcrumbs()

        return AlertDialog.Builder(activity)
                .setTitle(resources.getString(R.string.smtfp_select_destination))
                .setView(dialog)
                .setPositiveButton(R.string.smtfp_ok) { dialog, which -> sendResult() }
                .setNegativeButton(R.string.smtfp_cancel, null)
                .create()
    }

    private fun updateItems() {
        var items = getItems(mPath)
        if (!containsDirectory(items) && !mFirstUpdate) {
            sendResult()
            return
        }

        items = items.sortedWith(compareBy({ !it.isDirectory }, { it.name.toLowerCase() }))

        val adapter = ItemsAdapter(context, items)
        dialog.directory_picker_list.adapter = adapter
        dialog.directory_picker_breadcrumbs.setBreadcrumb(mPath, mShowFullPath)
        dialog.directory_picker_list.setOnItemClickListener { adapterView, view, position, id ->
            val item = items[position]
            if (item.isDirectory) {
                mPath = item.path
                updateItems()
            }
        }

        mFirstUpdate = false
    }

    private fun sendResult() {
        val intent = Intent()
        intent.data = Uri.parse(mPath)
        targetFragment.onActivityResult(requestCode, Activity.RESULT_OK, intent)
        dismiss()
    }

    private fun setupBreadcrumbs() {
        dialog.directory_picker_breadcrumbs.setListener(this)
    }

    private fun getItems(path: String): List<FileDirItem> {
        val items = ArrayList<FileDirItem>()
        val base = File(path)
        val files = base.listFiles()
        if (files != null) {
            for (file in files) {
                if (!file.isDirectory)
                    continue

                if (!mShowHidden && file.isHidden)
                    continue

                val curPath = file.absolutePath
                val curName = curPath.getFilenameFromPath()
                val size = file.length()

                items.add(FileDirItem(curPath, curName, file.isDirectory, getChildren(file), size))
            }
        }
        return items
    }

    private fun getChildren(file: File): Int {
        if (file.listFiles() == null || !file.isDirectory)
            return 0

        return file.listFiles().size
    }

    private fun containsDirectory(items: List<FileDirItem>): Boolean {
        for (item in items) {
            if (item.isDirectory) {
                return true
            }
        }
        return false
    }

    override fun breadcrumbClicked(id: Int) {
        val item = dialog.directory_picker_breadcrumbs.getChildAt(id).tag as FileDirItem
        mPath = item.path
        updateItems()
    }
}
