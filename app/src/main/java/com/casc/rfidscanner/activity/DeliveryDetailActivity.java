package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.GoodsAdapter;
import com.casc.rfidscanner.adapter.ProductAdapter;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeliveryDetailActivity extends BaseActivity {

    private static final String TAG = DeliveryDetailActivity.class.getSimpleName();

    @BindView(R.id.tv_detail_card_id) TextView mCardIDTV;
    @BindView(R.id.tv_detail_bill_id) TextView mBillIDTV;
    @BindView(R.id.tv_detail_delivery_count) TextView mDeliveryCountTV;
    @BindView(R.id.tv_detail_total_count) TextView mTotalCountTV;

    @BindView(R.id.rv_delivery_detail_goods) RecyclerView mGoodsRV;
    @BindView(R.id.rv_detail_products) RecyclerView mProductsRV;

    private GoodsAdapter goodsAdapter;
    private ProductAdapter productAdapter;
    private DeliveryBill mBill;

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, DeliveryDetailActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
        ((BaseActivity) context).overridePendingTransition(R.anim.push_bottom_in, 0);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        mDeliveryCountTV.setText(String.valueOf(mBill.getDeliveryCount()));
        goodsAdapter.notifyDataSetChanged();
        productAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_detail);
        ButterKnife.bind(this);

        mBill = MyVars.deliveryBillToShow;
        mCardIDTV.setText(mBill.getCardID());
        mBillIDTV.setText(mBill.getBillID());
        mDeliveryCountTV.setText(String.valueOf(mBill.getDeliveryCount()));
        mTotalCountTV.setText(String.valueOf(mBill.getTotalCount()));

        goodsAdapter = new GoodsAdapter(mBill.getGoods());
        mGoodsRV.setLayoutManager(new LinearLayoutManager(this));
        mGoodsRV.setAdapter(goodsAdapter);

        productAdapter = new ProductAdapter(mBill.getProducts());
        mProductsRV.setLayoutManager(new LinearLayoutManager(this));
        mProductsRV.setAdapter(productAdapter);
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

    @OnClick(R.id.btn_detail_confirm)
    void onConfirmButtonClicked() {
        String content;
        if (mBill.getDeliveryCount() < mBill.getTotalCount())
            content = "出库货物（不足）提货单数量，仍确认出库吗？";
        else if (mBill.getDeliveryCount() > mBill.getTotalCount())
            content = "出库货物（超过）提货单数量，仍确认出库吗？";
        else
            content = "出库货物：" + mBill.getDeliveryCount() + "（桶），确认出库吗？";
        new MaterialDialog.Builder(this)
                .title("提示信息")
                .content(content)
                .positiveText("确认")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消")
                .negativeColorRes(R.color.gray)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        EventBus.getDefault().post(new BillFinishedMessage());
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

    @OnClick(R.id.btn_detail_close)
    void onRootClicked() {
        finish();
    }
}
