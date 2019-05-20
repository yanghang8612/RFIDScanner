package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.BillConfirmedMessage;
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
        if (ActivityCollector.topNotOf(BillConfirmActivity.class)) {
            Intent intent = new Intent(context, BillConfirmActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    @BindView(R.id.spn_delivery_driver) BetterSpinner mDriverSpn;
    @BindView(R.id.spn_delivery_dealer) BetterSpinner mDealerSpn;

    @OnClick(R.id.btn_dialog_cancel) void onCancelButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_dialog_confirm) void onConfirmButtonClicked() {
        String driver = mDriverSpn.getText().toString();
        String dealer = mDealerSpn.getText().toString();
        if (TextUtils.isEmpty(driver) || TextUtils.isEmpty(dealer)) {
            showToast("请选择司机或经销商");
        } else {
            BillConfirmedMessage message = new BillConfirmedMessage();
            message.driver = driver;
            message.dealer = dealer;
            EventBus.getDefault().post(message);
            finish();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        updateConfigViews();
    }

    @Override
    protected void initActivity() {
        EventBus.getDefault().register(this);
        updateConfigViews();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_bill_confirm;
    }

    private void updateConfigViews() {
        mDriverSpn.setAdapter(new ArrayAdapter<>(this, R.layout.item_dialog_selection,
                MyVars.config.getDrivers()));
        mDealerSpn.setAdapter(new ArrayAdapter<>(this, R.layout.item_dialog_selection,
                MyVars.config.getDealers()));
    }
}
