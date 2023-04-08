package it.amonshore.comikkua.ui

import androidx.appcompat.view.ActionMode
import java.time.Duration

interface OnNavigationFragmentListener {
    fun onFragmentRequestActionMode(
        name: String,
        title: CharSequence? = null,
        callback: ActionMode.Callback? = null
    )

    fun handleUndo(message: String, tag: String, timeout: Duration = Duration.ZERO)

    fun resetUndo()
}