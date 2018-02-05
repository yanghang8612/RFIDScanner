package com.casc.rfidscanner.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.message.BatteryStatusMessage;
import com.casc.rfidscanner.message.LongTimeNoTouchMessage;

import org.greenrobot.eventbus.EventBus;

import java.lang.ref.WeakReference;

import butterknife.ButterKnife;

public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    // Constants for Message.what

    private Handler mHandler = new InnerHandler(this);

    private Fragment mCurFragment;

    private CountDownTimer mTouchTimer = new CountDownTimer(60 * 60 * 1000, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            EventBus.getDefault().post(new LongTimeNoTouchMessage());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;

                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    float batteryPct = level / (float)scale;

                    MyVars.batteryStatus = new BatteryStatusMessage(isCharging, batteryPct);
                    EventBus.getDefault().post(MyVars.batteryStatus);
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        switchFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        switchFragment();
    }

    private void switchFragment() {
        Class fragmentToSwitch = LinkType.getType().fragmentClass;
        if (mCurFragment != null && !fragmentToSwitch.equals(mCurFragment.getClass())) {
            getSupportFragmentManager().popBackStackImmediate();
        }
        if (mCurFragment == null || !fragmentToSwitch.equals(mCurFragment.getClass())) {
            try {
                mCurFragment = (Fragment) fragmentToSwitch.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fl_main_content, mCurFragment);
            transaction.commit();
        }
    }

    private static class InnerHandler extends Handler {

        private WeakReference<MainActivity> mOuter;

        InnerHandler(MainActivity activity) {
            this.mOuter = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity outer = mOuter.get();
            switch (msg.what) {
            }
        }
    }
}
