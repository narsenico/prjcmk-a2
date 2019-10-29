package it.amonshore.comikkua;

import android.util.Log;

public class LogHelper {

    private final static String TAG = "CMK";

    public static void d(String message) {
        Log.d(TAG, message);
    }

    public static void d(String message, Object... args) {
        Log.d(TAG, String.format(message, args));
    }

    public static void w(String message) {
        Log.w(TAG, message);
    }

    public static void w(String message, Object... args) {
        Log.w(TAG, String.format(message, args));
    }

    public static void e(String message, Exception error) {
        Log.e(TAG, message, error);
    }
}
