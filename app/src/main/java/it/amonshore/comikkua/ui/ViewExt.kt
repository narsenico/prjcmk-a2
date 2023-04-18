package it.amonshore.comikkua.ui

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.Window
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import it.amonshore.comikkua.UNDO_TIMEOUT
import java.time.Duration

fun Window.hideKeyboard() =
    (decorView.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).run {
        hideSoftInputFromWindow(decorView.windowToken, 0)
    }

fun Snackbar.onDismissed(block: () -> Unit): Snackbar =
    addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
        override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
            if (event == DISMISS_EVENT_TIMEOUT ||
                event == DISMISS_EVENT_MANUAL
            ) {
                block()
            }
        }
    })

fun Duration.toSnackbarTimeout(): Int =
    if (isZero) UNDO_TIMEOUT else toMillis().toInt()

fun Context.getDrawableFromResource(@DrawableRes res: Int, @ColorInt tint: Int): Drawable =
    ResourcesCompat.getDrawable(resources, res, theme)!!.mutate().apply { setTint(tint) }