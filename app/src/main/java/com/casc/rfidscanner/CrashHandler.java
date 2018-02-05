package com.casc.rfidscanner;

import android.util.Log;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static final String TAG = CrashHandler.class.getSimpleName();

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        Log.i(TAG, e.toString());
        e.printStackTrace();
    }
}
