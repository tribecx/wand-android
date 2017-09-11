package com.tunashields.wand.utils;

import android.util.Log;

import com.tunashields.wand.BuildConfig;

/**
 * Created by Irvin on 9/11/17.
 */

public class L {
    private static String TAG = "Wand";

    public static void debug(String message) {
        if (BuildConfig.DEBUG) Log.d(TAG, message);
    }

    public static void info(String message) {
        if (BuildConfig.DEBUG) Log.i(TAG, message);
    }

    public static void warning(String message) {
        if (BuildConfig.DEBUG) Log.w(TAG, message);
    }

    public static void error(String message) {
        if (BuildConfig.DEBUG) Log.e(TAG, message);
    }
}
