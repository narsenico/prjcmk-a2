package it.amonshore.comikkua.ui

import android.view.Menu
import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.appcompat.view.ActionMode

fun createActionModeCallback(
    @MenuRes menuRes: Int,
    onAction: (actionId: Int) -> Boolean,
    onDestroy: () -> Unit
): ActionMode.Callback {
    return object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(menuRes, menu)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean =
            false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
            onAction(item.itemId)

        override fun onDestroyActionMode(mode: ActionMode) = onDestroy()
    }
}