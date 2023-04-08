package it.amonshore.comikkua.ui

import android.app.Activity
import android.view.Window
import android.view.inputmethod.InputMethodManager

fun Window.hideKeyboard() =
    (decorView.context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager).run {
        hideSoftInputFromWindow(decorView.windowToken, 0)
    }