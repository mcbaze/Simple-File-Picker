package com.simplemobiletools.filepicker.views

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import com.simplemobiletools.filepicker.R
import com.simplemobiletools.filepicker.extensions.getInternalStoragePath
import com.simplemobiletools.filepicker.models.FileDirItem
import kotlinx.android.synthetic.main.smtfp_breadcrumb_item.view.*

class Breadcrumbs(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs), View.OnClickListener {
    private var mDeviceWidth: Int = 0

    private var mInflater: LayoutInflater
    private var mListener: BreadcrumbsListener? = null

    init {
        mInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mDeviceWidth = getDeviceWidth()
    }

    fun setListener(listener: BreadcrumbsListener) {
        mListener = listener
    }

    fun getDeviceWidth(): Int {
        val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
        val deviceDisplay = Point()
        display.getSize(deviceDisplay)
        return deviceDisplay.x
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val childRight = measuredWidth - paddingRight
        val childBottom = measuredHeight - paddingBottom
        val childHeight = childBottom - paddingTop

        val usableWidth = mDeviceWidth - paddingLeft - paddingRight
        var maxHeight = 0
        var curWidth: Int
        var curHeight: Int
        var curLeft = paddingLeft
        var curTop = paddingTop

        val cnt = childCount
        for (i in 0..cnt - 1) {
            val child = getChildAt(i)

            child.measure(MeasureSpec.makeMeasureSpec(usableWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST))
            curWidth = child.measuredWidth
            curHeight = child.measuredHeight

            if (curLeft + curWidth >= childRight) {
                curLeft = paddingLeft
                curTop += maxHeight
                maxHeight = 0
            }

            child.layout(curLeft, curTop, curLeft + curWidth, curTop + curHeight)
            if (maxHeight < curHeight)
                maxHeight = curHeight

            curLeft += curWidth
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val usableWidth = mDeviceWidth - paddingLeft - paddingRight
        var width = 0
        var rowHeight = 0
        var lines = 1

        val cnt = childCount
        for (i in 0..cnt - 1) {
            val child = getChildAt(i)
            measureChild(child, widthMeasureSpec, heightMeasureSpec)
            width += child.measuredWidth
            rowHeight = child.measuredHeight

            if (width / usableWidth > 0) {
                lines++
                width = child.measuredWidth
            }
        }

        val parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val calculatedHeight = paddingTop + paddingBottom + rowHeight * lines
        setMeasuredDimension(parentWidth, calculatedHeight)
    }

    fun setBreadcrumb(fullPath: String, basePath: String) {
        var currPath = basePath
        val tempPath = fullPath.replaceFirst(basePath, getStorageName(basePath))

        removeAllViewsInLayout()
        val dirs = tempPath.split("/".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()
        for (i in dirs.indices) {
            val dir = dirs[i]
            if (i > 0) {
                currPath += dir + "/"
            }

            if (dir.isEmpty())
                continue

            val item = FileDirItem(currPath, dir, true, 0, 0)
            addBreadcrumb(item, i > 0)
        }
    }

    private fun getStorageName(basePath: String): String {
        val id = when (basePath) {
            "/" -> R.string.smtfp_root
            context.getInternalStoragePath() -> R.string.smtfp_internal
            else -> R.string.smtfp_sd_card
        }
        return context.getString(id) + "/"
    }

    private fun addBreadcrumb(item: FileDirItem, addPrefix: Boolean) {
        val view = mInflater.inflate(R.layout.smtfp_breadcrumb_item, null, false)
        var textToAdd = item.name
        if (addPrefix)
            textToAdd = " -> " + textToAdd

        view.breadcrumb_text.text = textToAdd
        addView(view)
        view.setOnClickListener(this)

        view.tag = item
    }

    fun removeBreadcrumb() {
        removeView(getChildAt(childCount - 1))
    }

    val lastItem: FileDirItem get() = getChildAt(childCount - 1).tag as FileDirItem

    override fun onClick(v: View) {
        val cnt = childCount
        for (i in 0..cnt - 1) {
            if (getChildAt(i) != null && getChildAt(i) == v) {
                mListener?.breadcrumbClicked(i)
            }
        }
    }

    interface BreadcrumbsListener {
        fun breadcrumbClicked(id: Int)
    }
}
