package com.casc.rfidscanner;

import android.app.Application;

import org.xutils.x;

public class MyApplication extends Application {

    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        x.Ext.init(this);
    }
}
