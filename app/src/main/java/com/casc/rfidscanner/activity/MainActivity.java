package com.casc.rfidscanner.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.LinkType;

public class MainActivity extends BaseActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Fragment mCurFragment;

    @Override
    protected void initActivity() {
        switchFragment();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_main;
    }

    @Override
    public void onResume() {
        super.onResume();
        switchFragment();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyVars.executor.shutdown();
        MyVars.usbReader.shutdown();
        MyVars.bleReader.shutdown();
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

}
