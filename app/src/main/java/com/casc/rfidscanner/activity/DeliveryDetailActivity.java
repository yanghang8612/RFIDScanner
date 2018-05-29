package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.BucketAdapter;
import com.casc.rfidscanner.adapter.GoodsAdapter;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeliveryDetailActivity extends BaseActivity {

    private static final String TAG = DeliveryDetailActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, DeliveryDetailActivity.class);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        context.startActivity(intent);
        ((BaseActivity) context).overridePendingTransition(R.anim.push_right_in, 0);
    }

    @BindView(R.id.tv_delivery_detail_card_id) TextView mCardIDTv;
    @BindView(R.id.tv_delivery_detail_bill_id) TextView mBillIDTv;
    @BindView(R.id.tv_delivery_detail_delivery_count) TextView mDeliveryCountTv;
    @BindView(R.id.tv_delivery_detail_total_count) TextView mTotalCountTv;

    @BindView(R.id.spn_delivery_dealer) BetterSpinner mDealerSpn;
    @BindView(R.id.spn_delivery_driver) BetterSpinner mDriverSpn;

    @BindView(R.id.vf_delivery_detail_content) ViewFlipper mContentVf;
    @BindView(R.id.rv_delivery_detail_goods) RecyclerView mGoodsRv;
    @BindView(R.id.rv_delivery_detail_buckets) RecyclerView mBucketsRv;

    @BindView(R.id.btn_detail_view_buckets) Button mViewBucketsBtn;
    @BindView(R.id.btn_detail_view_brief) Button mViewBriefBtn;
    @BindView(R.id.btn_detail_confirm) Button mDetailConfirmBtn;

    private GoodsAdapter mGoodsAdapter;
    private BucketAdapter mBucketAdapter;
    private DeliveryBill mBill;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        mDetailConfirmBtn.setEnabled(!mBill.isHighlight());
        mDeliveryCountTv.setText(String.valueOf(mBill.getDeliveryCount()));
        mGoodsAdapter.notifyDataSetChanged();
        mBucketAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_detail);
        ButterKnife.bind(this);

        mBill = MyVars.deliveryBillToShow;
        mDetailConfirmBtn.setEnabled(!mBill.isHighlight());
        mCardIDTv.setText(mBill.getCardID());
        mBillIDTv.setText(TextUtils.isEmpty(mBill.getBillID()) ? "待补单" : mBill.getBillID());
        mDeliveryCountTv.setText(String.valueOf(mBill.getDeliveryCount()));
        mTotalCountTv.setText(String.valueOf(mBill.getTotalCount()));

        mGoodsAdapter = new GoodsAdapter(mBill.getGoods());
        mGoodsRv.setLayoutManager(new LinearLayoutManager(this));
        mGoodsRv.setAdapter(mGoodsAdapter);

        mBucketAdapter = new BucketAdapter(mBill.getBuckets());
        mBucketsRv.setLayoutManager(new LinearLayoutManager(this));
        mBucketsRv.setAdapter(mBucketAdapter);

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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        if (!isTouchPointInView(findViewById(R.id.ll_delivery_detail_content), x, y)) {
            finish();
        }
        return super.dispatchTouchEvent(ev);
    }

    @OnClick(R.id.btn_detail_close)
    void onCloseButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_detail_view_buckets)
    void onViewBucketsButtonClicked() {
        mViewBucketsBtn.setVisibility(View.GONE);
        mViewBriefBtn.setVisibility(View.VISIBLE);
        mContentVf.setInAnimation(this, R.anim.push_right_in);
        mContentVf.setOutAnimation(this, R.anim.push_left_out);
        mContentVf.showNext();
    }

    @OnClick(R.id.btn_detail_view_brief)
    void onViewBriefButtonClicked() {
        mViewBucketsBtn.setVisibility(View.VISIBLE);
        mViewBriefBtn.setVisibility(View.GONE);
        mContentVf.setInAnimation(this, R.anim.push_left_in);
        mContentVf.setOutAnimation(this, R.anim.push_right_out);
        mContentVf.showPrevious();
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
}
