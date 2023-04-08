package it.amonshore.comikkua

import android.util.Log

object LogHelperKt {

    private const val TAG = "CMK"

    fun d(supplier: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, supplier())
        }
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String) {
        Log.w(TAG, message)
    }

    fun e(message: String) {
        Log.e(TAG, message)
    }

    fun e(message: String, error: Throwable) {
        Log.e(TAG, message, error)
    }

    fun e(error: Throwable) {
        Log.e(TAG, null, error)
    }
}