package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.casc.rfidscanner.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class DialogActivity extends BaseActivity {

    private static final String TAG = DialogActivity.class.getSimpleName();

    public static void actionStart(Context context, String content) {
        Intent intent = new Intent(context, DialogActivity.class);
        intent.putExtra("content", content);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
        //((BaseActivity) context).overridePendingTransition(R.anim.push_right_in, 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_dialog_cancel)
    void onCancelButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_dialog_confirm)
    void onConfirmButtonClicked() {
        finish();
    }
}
