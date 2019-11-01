package it.amonshore.comikkua;

import android.util.Log;

import androidx.annotation.NonNull;

public class LogHelper {

    private static String mTag = "CMK";

    public static void setTag(@NonNull String tag) {
        mTag = tag;
    }

    public static void d(String message) {
        Log.d(mTag, message);
    }

    public static void d(String message, Object... args) {
        Log.d(mTag, String.format(message, args));
    }

    public static void w(String message) {
        Log.w(mTag, message);
    }

    public static void w(String message, Object... args) {
        Log.w(mTag, String.format(message, args));
    }

    public static void e(String message, Exception error) {
        Log.e(mTag, message, error);
    }
}
