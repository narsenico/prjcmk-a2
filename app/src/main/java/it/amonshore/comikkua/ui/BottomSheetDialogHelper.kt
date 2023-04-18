package it.amonshore.comikkua.ui

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.children
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetDialog

private const val TITLE_TAG = "BottomSheetDialogHelper.Title"
private const val CHILD_TAG = "BottomSheetDialogHelper"

fun showBottomSheetDialog(
    activity: FragmentActivity,
    @LayoutRes layout: Int,
    title: String,
    onClick: (id: Int) -> Unit
) {
    val sheetView = activity.layoutInflater.inflate(layout, null) as ViewGroup
    val bottomSheetDialog = BottomSheetDialog(activity).apply {
        setContentView(sheetView)
    }

    sheetView.findViewWithTag<TextView>(TITLE_TAG)?.apply {
        text = title
        visibility = View.VISIBLE
    }

    sheetView.children
        .filter { it.tag == CHILD_TAG }
        .forEach { child ->
            child.setOnClickListener {
                bottomSheetDialog.dismissAndCall(child.id, onClick)
            }
        }

    bottomSheetDialog.show()
}

inline fun BottomSheetDialog.dismissAndCall(id: Int, onClick: (id: Int) -> Unit) {
    dismiss()
    onClick(id)
}