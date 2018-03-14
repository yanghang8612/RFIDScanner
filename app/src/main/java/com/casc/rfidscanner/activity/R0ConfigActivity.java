package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.message.ConfigChangedMessage;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class R0ConfigActivity extends BaseActivity {

    private static final String TAG = R0ConfigActivity.class.getSimpleName();

    public static void actionStart(Context context, String[] configs) {
        Intent intent = new Intent(context, R0ConfigActivity.class);
        intent.putExtra("configs", configs);
        context.startActivity(intent);
        ((BaseActivity) context).overridePendingTransition(R.anim.push_right_in, 0);
    }

    @BindView(R.id.spn_bucket_spec) BetterSpinner mBucketSpecSpn;
    @BindView(R.id.spn_bucket_type) BetterSpinner mBucketTypeSpn;
    @BindView(R.id.spn_water_brand) BetterSpinner mWaterBrandSpn;
    @BindView(R.id.spn_water_spec) BetterSpinner mWaterSpecSpn;
    @BindView(R.id.spn_bucket_producer) BetterSpinner mBucketProducerSpn;
    @BindView(R.id.spn_bucket_owner) BetterSpinner mBucketOwnerSpn;
    @BindView(R.id.spn_bucket_user) BetterSpinner mBucketUserSpn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r0_config);
        ButterKnife.bind(this);
        String[] configs = getIntent().getStringArrayExtra("configs");
        mBucketSpecSpn.setText(configs[0]);
        mBucketTypeSpn.setText(configs[1]);
        mWaterBrandSpn.setText(configs[2]);
        mWaterSpecSpn.setText(configs[3]);
        mBucketProducerSpn.setText(configs[4]);
        mBucketOwnerSpn.setText(configs[5]);
        mBucketUserSpn.setText(configs[6]);
        updateConfigViews();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.push_right_out);
    }

    @OnClick(R.id.btn_r0_config_confirm)
    void onConfirmButtonClicked() {
        ConfigChangedMessage message = new ConfigChangedMessage();
        message.bucketSpec = mBucketSpecSpn.getText().toString();
        message.bucketType = mBucketTypeSpn.getText().toString();
        message.waterBrand = mWaterBrandSpn.getText().toString();
        message.waterSpec = mWaterSpecSpn.getText().toString();
        message.bucketProducer = mBucketProducerSpn.getText().toString();
        message.bucketOwner = mBucketOwnerSpn.getText().toString();
        message.bucketUser = mBucketUserSpn.getText().toString();
        EventBus.getDefault().post(message);
        finish();
    }

    @OnClick(R.id.btn_r0_config_cancel)
    void onCancelButtonClicked() {
        finish();
    }

    private void updateConfigViews() {
        mBucketSpecSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getBucketSpecInfo()));

        mBucketTypeSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getBucketTypeInfo()));

        mWaterBrandSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getWaterBrandInfo()));

        mWaterSpecSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getWaterSpecInfo()));

        mBucketProducerSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getBucketProducerInfo()));

        mBucketOwnerSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getBucketOwnerInfo()));

        mBucketUserSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_specify, MyVars.config.getBucketUserInfo()));
    }
}
