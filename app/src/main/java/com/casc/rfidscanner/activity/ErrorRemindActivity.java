package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.casc.rfidscanner.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ErrorRemindActivity extends BaseActivity {

    private static final String TAG = ErrorRemindActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, ErrorRemindActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    @OnClick(R.id.btn_reset) void onResetButtonClicked() {
        finish();
    }

    @Override
    protected void initActivity() {

    }

    @Override
    protected int getLayout() {
        return R.layout.activity_error_remind;
    }

}
