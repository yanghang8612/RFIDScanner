package com.casc.rfidscanner.activity;

import android.os.Bundle;

import com.casc.rfidscanner.R;

public class SplashActivity extends BaseActivity {

    private static final String TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }
}
