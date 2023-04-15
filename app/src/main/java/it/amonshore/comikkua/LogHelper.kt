package it.amonshore.comikkua

import android.util.Log

object LogHelper {

    private const val TAG = "CMK"

    @Deprecated("Usare versione con supplier")
    @JvmStatic
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    @JvmStatic
    fun d(supplier: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, supplier())
        }
    }

    @JvmStatic
    fun i(message: String) {
        Log.i(TAG, message)
    }

    @JvmStatic
    fun w(message: String) {
        Log.w(TAG, message)
    }

    @JvmStatic
    fun e(message: String) {
        Log.e(TAG, message)
    }

    @JvmStatic
    fun e(message: String, error: Throwable? = null) {
        Log.e(TAG, message, error)
    }

    @JvmStatic
    fun e(error: Throwable) {
        Log.e(TAG, null, error)
    }
}