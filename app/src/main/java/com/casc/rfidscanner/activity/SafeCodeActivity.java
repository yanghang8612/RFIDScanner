package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.widget.CardView;
import android.widget.TextView;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.view.InputCodeLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SafeCodeActivity extends BaseActivity {

    private static final String TAG = SafeCodeActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        if (ActivityCollector.topNotOf(SafeCodeActivity.class)) {
            Intent intent = new Intent(context, SafeCodeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    // 安全码
    private StringBuilder mSafeCode;

    // 系统震动辅助类
    private Vibrator mVibrator;

    @BindView(R.id.icl_safe_code) InputCodeLayout mSafeCodeIcl;

    @OnClick({
            R.id.cv_keyboard_one, R.id.cv_keyboard_two, R.id.cv_keyboard_three,
            R.id.cv_keyboard_four, R.id.cv_keyboard_five, R.id.cv_keyboard_six,
            R.id.cv_keyboard_seven, R.id.cv_keyboard_eight, R.id.cv_keyboard_nine,
            R.id.cv_keyboard_zero})
    void onKeyboardClicked(CardView view) {
        mVibrator.vibrate(30);
        TextView textView = (TextView) view.getChildAt(0);
        mSafeCode.append(textView.getText().toString());
        mSafeCodeIcl.addCode("*");
    }

    @OnClick(R.id.cv_keyboard_clear) void onKeyboardClearClicked() {
        mVibrator.vibrate(80);
        mSafeCode.delete(0, mSafeCode.length());
        mSafeCodeIcl.clear();
    }

    @OnClick(R.id.cv_keyboard_back) void onKeyboardBackClicked() {
        mVibrator.vibrate(50);
        if (mSafeCode.length() != 0) {
            mSafeCode.deleteCharAt(mSafeCode.length() - 1);
        }
        mSafeCodeIcl.deleteCode();
    }

    @Override
    protected void initActivity() {
        mSafeCode = new StringBuilder();
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        MyVars.executor.execute(new SafeCodeCheck());
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_safe_code;
    }

    private class SafeCodeCheck implements Runnable {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 5000L) {
                if ("8612".equals(mSafeCode.toString())) {
                    ConfigActivity.actionStart(ActivityCollector.getTopActivity(), "");
                    break;
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            finish();
        }
    }
}
