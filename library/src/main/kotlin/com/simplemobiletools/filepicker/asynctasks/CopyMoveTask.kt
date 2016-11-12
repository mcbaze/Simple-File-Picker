package com.simplemobiletools.filepicker.asynctasks

import android.content.Context
import android.os.AsyncTask
import android.support.v4.util.Pair
import android.util.Log
import com.simplemobiletools.filepicker.extensions.getFileDocument
import com.simplemobiletools.filepicker.extensions.needsStupidWritePermissions
import com.simplemobiletools.filepicker.extensions.scanFile
import com.simplemobiletools.filepicker.extensions.scanFiles
import java.io.*
import java.lang.ref.WeakReference
import java.util.*

class CopyMoveTask(val context: Context, val deleteAfterCopy: Boolean = false, val treeUri: String = "", listener: CopyMoveTask.CopyMoveListener) : AsyncTask<Pair<ArrayList<File>, File>, Void, Boolean>() {
    private val TAG = CopyMoveTask::class.java.simpleName
    private var mListener: WeakReference<CopyMoveListener>? = null
    private var mMovedFiles: ArrayList<File>

    init {
        mListener = WeakReference(listener)
        mMovedFiles = arrayListOf()
    }

    override fun doInBackground(vararg params: Pair<ArrayList<File>, File>): Boolean? {
        val pair = params[0]
        val files = pair.first
        for (file in files) {
            try {
                val curFile = File(pair.second, file.name)
                if (curFile.exists())
                    continue

                copy(file, curFile)
            } catch (e: Exception) {
                Log.e(TAG, "copy $e")
                return false
            }
        }

        if (deleteAfterCopy) {
            for (file in mMovedFiles) {
                if (context.needsStupidWritePermissions(file.absolutePath)) {
                    context.getFileDocument(file.absolutePath, treeUri)
                } else {
                    file.delete()
                }
            }
        }
        context.scanFiles(files) {}
        context.scanFiles(mMovedFiles) {}
        return true
    }

    @Throws(Exception::class)
    private fun copy(source: File, destination: File) {
        if (source.isDirectory) {
            copyDirectory(source, destination)
        } else {
            copyFile(source, destination)
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            if (context.needsStupidWritePermissions(destination.absolutePath)) {
                val document = context.getFileDocument(destination.absolutePath, treeUri)
                document.createDirectory(destination.name)
            } else if (!destination.mkdirs()) {
                throw IOException("Could not create dir ${destination.absolutePath}")
            }
        }

        val children = source.list()
        for (child in children) {
            val newFile = File(source, child)
            if (context.needsStupidWritePermissions(destination.absolutePath)) {
                if (newFile.isDirectory) {
                    copyDirectory(newFile, File(destination, child))
                } else {
                    var document = context.getFileDocument(destination.absolutePath, treeUri)
                    document = document.createFile("", child)

                    val inputStream = FileInputStream(newFile)
                    val out = context.contentResolver.openOutputStream(document.uri)
                    copyStream(inputStream, out)
                    context.scanFile(destination) {}
                    mMovedFiles.add(source)
                }
            } else {
                copy(newFile, File(destination, child))
            }
        }
    }

    private fun copyFile(source: File, destination: File) {
        val directory = destination.parentFile
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create dir ${directory.absolutePath}")
        }

        val inputStream = FileInputStream(source)
        val out: OutputStream?
        if (context.needsStupidWritePermissions(destination.absolutePath)) {
            var document = context.getFileDocument(destination.absolutePath, treeUri)
            document = document.createFile("", destination.name)

            out = context.contentResolver.openOutputStream(document.uri)
        } else {
            out = FileOutputStream(destination)
        }

        copyStream(inputStream, out)
        context.scanFile(destination) {}
        mMovedFiles.add(source)
    }

    private fun copyStream(inputStream: InputStream, out: OutputStream?) {
        val buf = ByteArray(1024)
        var len: Int
        while (true) {
            len = inputStream.read(buf)
            if (len <= 0)
                break
            out?.write(buf, 0, len)
        }
    }

    override fun onPostExecute(success: Boolean) {
        val listener = mListener?.get() ?: return

        if (success) {
            listener.copySucceeded(deleteAfterCopy)
        } else {
            listener.copyFailed()
        }
    }

    interface CopyMoveListener {
        fun copySucceeded(deleted: Boolean)

        fun copyFailed()
    }
}
