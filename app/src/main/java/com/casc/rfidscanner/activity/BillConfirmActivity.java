package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BillConfirmActivity extends BaseActivity {

    private static final String TAG = BillConfirmActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, BillConfirmActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
    }

    @BindView(R.id.spn_delivery_driver) BetterSpinner mDriverSpn;
    @BindView(R.id.spn_delivery_dealer) BetterSpinner mDealerSpn;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        updateConfigViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_confirm);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        updateConfigViews();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent ev) {
//        int x = (int) ev.getRawX();
//        int y = (int) ev.getRawY();
//        if (!isTouchPointInView(findViewById(R.id.cv_bill_confirm_content), x, y)) {
//            finish();
//        }
//        return super.dispatchTouchEvent(ev);
//    }

    @OnClick(R.id.btn_dialog_cancel)
    void onCancelButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_dialog_confirm)
    void onConfirmButtonClicked() {
        BillFinishedMessage message = new BillFinishedMessage();
        message.driver = mDriverSpn.getText().toString();
        message.dealer = mDealerSpn.getText().toString();
        EventBus.getDefault().post(message);
        finish();
    }

    private void updateConfigViews() {
        mDriverSpn.setAdapter(new ArrayAdapter<>(this, R.layout.item_dialog_selection,
                MyVars.config.getDriverInfo()));
        mDealerSpn.setAdapter(new ArrayAdapter<>(this, R.layout.item_dialog_selection,
                MyVars.config.getDealerInfo()));
    }
}
