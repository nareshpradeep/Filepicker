package com.digitral.filepicker.utils;

import android.content.Context;
import android.util.Log;

public class TraceUtils {

    private TraceUtils() {
        //Empty constructorTemplate
    }

    public static void logThrowable(Throwable e) {

        e.printStackTrace();
    }

    public static void logException(Exception e) {
        e.printStackTrace();
    }

    public static void logException(Context context, Exception e) {
        e.printStackTrace();
    }

    public static void logE(String key, String value) {
        Log.e(key, value);
    }

}