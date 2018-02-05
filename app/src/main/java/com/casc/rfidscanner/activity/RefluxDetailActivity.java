package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.GoodsAdapter;
import com.casc.rfidscanner.adapter.ProductAdapter;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class RefluxDetailActivity extends BaseActivity {

    private static final String TAG = RefluxDetailActivity.class.getSimpleName();

    @BindView(R.id.tv_reflux_detail_card_id) TextView mCardIDTV;
    @BindView(R.id.tv_reflux_detail_count) TextView mRefluxCountTV;

    @BindView(R.id.spn_dealer) BetterSpinner mDealerSpn;
    @BindView(R.id.spn_driver) BetterSpinner mDriverSpn;

    @BindView(R.id.rv_reflux_detail_goods) RecyclerView mGoodsRV;
    @BindView(R.id.rv_reflux_detail_buckets) RecyclerView mBucketsRV;

    private GoodsAdapter goodsAdapter;
    private ProductAdapter productAdapter;
    private RefluxBill mBill;

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, RefluxDetailActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
        ((BaseActivity) context).overridePendingTransition(R.anim.push_bottom_in, 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        mRefluxCountTV.setText(String.valueOf(mBill.getRefluxCount()));
        goodsAdapter.notifyDataSetChanged();
        productAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reflux_detail);
        ButterKnife.bind(this);

        mBill = MyVars.refluxBillToShow;
        mCardIDTV.setText(mBill.getCardID());
        mRefluxCountTV.setText(String.valueOf(mBill.getRefluxCount()));

        goodsAdapter = new GoodsAdapter(mBill.getGoods());
        mGoodsRV.setLayoutManager(new LinearLayoutManager(this));
        mGoodsRV.setAdapter(goodsAdapter);

        productAdapter = new ProductAdapter(mBill.getBuckets());
        mBucketsRV.setLayoutManager(new LinearLayoutManager(this));
        mBucketsRV.setAdapter(productAdapter);

        if (!MyVars.config.getDealerInfo().isEmpty())
            mDealerSpn.setText(MyVars.config.getDealerInfo().get(0));

        mDealerSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getDealerInfo()));

        if (!MyVars.config.getDriverInfo().isEmpty())
            mDriverSpn.setText(MyVars.config.getDriverInfo().get(0));

        mDriverSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getDriverInfo()));
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.push_bottom_out);
    }

    @OnClick(R.id.btn_reflux_detail_confirm)
    void onConfirmButtonClicked() {
        new MaterialDialog.Builder(this)
                .title("提示信息")
                .content("回流空桶：" + mBill.getRefluxCount() + "（桶），确认回流吗？")
                .positiveText("确认")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消")
                .negativeColorRes(R.color.gray)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        BillFinishedMessage message = new BillFinishedMessage();
                        message.dealer = mDealerSpn.getText().toString();
                        message.driver = mDriverSpn.getText().toString();
                        EventBus.getDefault().post(message);
                        dialog.dismiss();
                        finish();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.btn_reflux_detail_close)
    void onRootClicked() {
        finish();
    }
}
