package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.BillConfirmActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.StackDetailActivity;
import com.casc.rfidscanner.adapter.DeliveryBillAdapter;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.bean.Stack;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.param.MsgDelivery;
import com.casc.rfidscanner.helper.net.param.MsgLog;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.BillConfirmedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.StackDeletedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnLongClick;

/**
 * 成品水出库Fragment
 */
public class R6Fragment extends BaseFragment {

    private static final String TAG = R6Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_STACKS = 0;
    private static final int MSG_UPDATE_BILLS = 1;

    // 出库单列表
    private List<DeliveryBill> mBills = new ArrayList<>();

    // 出库单map，用于根据出货单EPC获取出库单实例
    private Map<String, DeliveryBill> mBillsMap = new HashMap<>();

    // 出库单列表适配器
    private DeliveryBillAdapter mBillAdapter;

    private DeliveryBill mSelectedBill, mCurBill;

    private CardView mSelectedBillView, mSelectedStackView, mStackViewToDelete;

    private List<String> mUnidentifiedBuckets = new LinkedList<>();

    private List<String> mStack1Buckets = new LinkedList<>();

    private List<String> mStack2Buckets = new LinkedList<>();

    private List<String> mBulkBuckets = new LinkedList<>();

    private List<String> mCache = new LinkedList<>();

    private List<String> mErrors = new LinkedList<>();

    private final Object mLock = new Object();

    private int mStack1Id, mStack2Id;

    // 错误已提示标识
    private boolean mIsErrorNoticed, mIsBacking;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @BindView(R.id.tv_r6_title) TextView mTitleTv;
    @BindView(R.id.rv_r6_bills) RecyclerView mBillsRv;
    @BindView(R.id.cv_r6_stack_1) CardView mStack1Cv;
    @BindView(R.id.ns_r6_stack_1_count) NumberSwitcher mStack1CountNs;
    @BindView(R.id.cv_r6_stack_2) CardView mStack2Cv;
    @BindView(R.id.ns_r6_stack_2_count) NumberSwitcher mStack2CountNs;
    @BindView(R.id.cv_r6_bulk) CardView mBulkCv;
    @BindView(R.id.ns_r6_bulk_count) NumberSwitcher mBulkCountNs;
    @BindView(R.id.cv_r6_scanned) CardView mScannedCv;
    @BindView(R.id.ns_r6_scanned_count) NumberSwitcher mScannedCountNs;

    @OnClick(R.id.tv_r6_title) void onTitleClicked() {
        if (!mIsBacking) {
            mIsBacking = true;
            mTitleTv.setText("成品退库");
            mTitleTv.getBackground().setTint(mContext.getColor(R.color.red));
        } else {
            mIsBacking = false;
            mTitleTv.setText("成品出库");
            mTitleTv.getBackground().setTint(mContext.getColor(R.color.white));
        }
    }

    @OnClick({R.id.cv_r6_stack_1, R.id.cv_r6_stack_2, R.id.cv_r6_bulk, R.id.cv_r6_scanned})
    void onStackClicked(CardView view) {
        if (mSelectedStackView == view) {
            mSelectedStackView.setCardBackgroundColor(mContext.getColor(R.color.snow));
            mSelectedStackView = null;
        } else {
            if (mSelectedStackView != null) {
                mSelectedStackView.setCardBackgroundColor(mContext.getColor(R.color.snow));
            }
            mSelectedStackView = view;
            mSelectedStackView.setCardBackgroundColor(mContext.getColor(R.color.wheat));
        }
        merge();
    }

    @OnLongClick({R.id.cv_r6_stack_1, R.id.cv_r6_stack_2, R.id.cv_r6_bulk, R.id.cv_r6_scanned})
    boolean onStackLongClicked(CardView view) {
        mVibrator.vibrate(50);
        mStackViewToDelete = view;
        if (mStackViewToDelete == mStack1Cv) {
            MyVars.stackToShow = new Stack(mStack1Id, mStack1Buckets);
        } else if (mStackViewToDelete == mStack2Cv) {
            MyVars.stackToShow = new Stack(mStack2Id, mStack2Buckets);
        } else if (mStackViewToDelete == mBulkCv) {
            MyVars.stackToShow = new Stack(mBulkBuckets);
        } else {
            MyVars.stackToShow = new Stack(mUnidentifiedBuckets);
        }
        StackDetailActivity.actionStart(mContext);
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(StackDeletedMessage message) {
        synchronized (mLock) {
            if (mStackViewToDelete == mStack1Cv) {
                mStack1Buckets.clear();
            } else if (mStackViewToDelete == mStack2Cv) {
                mStack2Buckets.clear();
            } else if (mStackViewToDelete == mBulkCv) {
                mBulkBuckets.clear();
            } else {
                mUnidentifiedBuckets.clear();
            }
            if (mSelectedStackView != null) {
                mSelectedStackView.setCardBackgroundColor(mContext.getColor(R.color.snow));
            }
            mSelectedStackView = mStackViewToDelete = null;
            Message.obtain(mHandler, MSG_UPDATE_STACKS).sendToTarget();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillConfirmedMessage message) {
        // 异步上传平台
        MsgDelivery delivery = new MsgDelivery(
                mCurBill.isFromCard() ? "0000000000" : mCurBill.getBillID(),
                mCurBill.isFromCard() ? 2 : mCurBill.checkBill() ?  0 : 1,
                message.dealer, message.driver);
        for (Map.Entry<String, Long> bucket : mCurBill.getBuckets().entrySet()) {
            delivery.addBucket(bucket.getKey(), bucket.getValue(), "0");
        }
        for (Map.Entry<String, Long> bucket : mCurBill.getRemoves().entrySet()) {
            delivery.addBucket(bucket.getKey(), bucket.getValue(), "2");
        }
        MyVars.cache.storeDeliveryBill(delivery);
        showToast("提交成功");
        if (mCurBill == mSelectedBill) {
            mSelectedBillView.setCardBackgroundColor(mContext.getColor(R.color.snow));
            mSelectedBillView = null;
            mSelectedBill = null;
        }
        mBills.remove(mCurBill);
        mBillsMap.remove(mCurBill.getBillID());
        Message.obtain(mHandler, MSG_UPDATE_BILLS).sendToTarget();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case BUCKET:
                    if (mIsBacking) {
                        if (mSelectedBill != null && mSelectedBill.removeBucket(epcStr)) {
                            synchronized (mLock) {
                                mCache.remove(epcStr);
                            }
                            Message.obtain(mHandler, MSG_UPDATE_BILLS).sendToTarget();
                        }
                    } else {
                        synchronized (mLock) {
                            if (!mCache.contains(epcStr)
                                    && !mStack1Buckets.contains(epcStr) && !mStack2Buckets.contains(epcStr)
                                    && !mBulkBuckets.contains(epcStr) && !mUnidentifiedBuckets.contains(epcStr)) {
                                mUnidentifiedBuckets.add(epcStr);
                                Message.obtain(mHandler, MSG_UPDATE_STACKS).sendToTarget();
                            }
                            if (mCache.contains(epcStr) && !mErrors.contains(epcStr)) {
                                mErrors.add(epcStr);
                                MyVars.cache.storeLogMessage(MsgLog.warn("重复出库: " + CommonUtils.getBodyCode(epcStr)));
                            }
                        }
                    }
                    break;
                case CARD_DELIVERY:
                    if (!mBillsMap.containsKey(DeliveryBill.getCardIDFromEPC(message.epc))) {
                        DeliveryBill bill = new DeliveryBill(message.epc);
                        mBills.add(0, bill);
                        mBillsMap.put(bill.getBillID(), bill);
                        Message.obtain(mHandler, MSG_UPDATE_BILLS).sendToTarget();
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        ConfigActivity.actionStart(mContext, epcStr);
                    }
                    break;
            }
        } else {
            mAdminCardScannedCount = 0;
        }
    }

    @Override
    protected void initFragment() {
        mStack1CountNs.setNumber(0);
        mStack2CountNs.setNumber(0);
        mBulkCountNs.setNumber(0);
        mScannedCountNs.setNumber(0);
        mBillAdapter = new DeliveryBillAdapter(mContext, mBills);
        mBillAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (mSelectedBillView == view) {
                    mSelectedBillView.setCardBackgroundColor(mContext.getColor(R.color.snow));
                    mSelectedBillView = null;
                    mSelectedBill = null;
                } else {
                    if (mSelectedBillView != null) {
                        mSelectedBillView.setCardBackgroundColor(mContext.getColor(R.color.snow));
                    }
                    mSelectedBill = mBills.get(position);
                    mSelectedBillView = (CardView) view;
                    mSelectedBillView.setCardBackgroundColor(mContext.getColor(R.color.wheat));
                }
                merge();
            }
        });
        mBillAdapter.setOnItemLongClickListener(new BaseQuickAdapter.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(BaseQuickAdapter adapter, View view, int position) {
                mVibrator.vibrate(50);
                mCurBill = mBills.get(position);
                BillConfirmActivity.actionStart(mContext);
                return true;
            }
        });
        mBillAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (view.getId() == R.id.btn_delivery_bill_cancel) {
                    final DeliveryBill bill = mBills.get(position);
                    if (bill.getStacks().size() == 1) {
                        showToast("散货撤销请走退库流程");
                    } else if (bill.getStacks().size() != 0) {
                        showDialog("确认要撤销上一垛出库吗？", new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                mCache.removeAll(bill.removeLastStack().getBuckets());
                                Message.obtain(mHandler, MSG_UPDATE_STACKS).sendToTarget();
                                Message.obtain(mHandler, MSG_UPDATE_BILLS).sendToTarget();
                            }
                        });
                    }
                }
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mBillsRv.setLayoutManager(layoutManager);
        mBillsRv.setAdapter(mBillAdapter);
        MyVars.executor.scheduleWithFixedDelay(new IdentifyBucketTask(), 3000, 10, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new DeliveryBillQueryTask(), 3, 10, TimeUnit.SECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6;
    }

    private void merge() {
        if (mSelectedStackView != null && mSelectedBillView != null) {
            synchronized (mLock) {
                List<String> buckets;
                if (mSelectedStackView == mStack1Cv) {
                    buckets = mStack1Buckets;
                    mSelectedBill.addStack(new Stack(mStack1Id, buckets));
                } else if (mSelectedStackView == mStack2Cv) {
                    buckets = mStack2Buckets;
                    mSelectedBill.addStack(new Stack(mStack2Id, buckets));
                } else {
                    if (mSelectedStackView == mBulkCv) {
                        buckets = mBulkBuckets;
                    } else {
                        buckets = mUnidentifiedBuckets;
                    }
                    for (String bucket : buckets) {
                        mSelectedBill.addBulk(bucket);
                    }
                }
                mCache.addAll(buckets);
                mSelectedStackView.setCardBackgroundColor(mContext.getColor(R.color.snow));
                mSelectedStackView = null;
                mSelectedBillView.setCardBackgroundColor(mContext.getColor(R.color.snow));
                mSelectedBillView = null;
                mSelectedBill = null;
                buckets.clear();
                Message.obtain(mHandler, MSG_UPDATE_STACKS).sendToTarget();
                Message.obtain(mHandler, MSG_UPDATE_BILLS).sendToTarget();
            }
        }
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R6Fragment> mOuter;

        InnerHandler(R6Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R6Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_UPDATE_STACKS:
                    outer.mStack1Cv.setVisibility(outer.mStack1Buckets.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    outer.mStack2Cv.setVisibility(outer.mStack2Buckets.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    outer.mBulkCv.setVisibility(outer.mBulkBuckets.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    outer.mStack1CountNs.setNumber(outer.mStack1Buckets.size());
                    outer.mStack2CountNs.setNumber(outer.mStack2Buckets.size());
                    outer.mBulkCountNs.setNumber(outer.mBulkBuckets.size());
                    outer.mScannedCountNs.setNumber(outer.mUnidentifiedBuckets.size());
                    break;
                case MSG_UPDATE_BILLS:
                    outer.mBillAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    private class IdentifyBucketTask implements Runnable {

        private class StackInfo {

            @SerializedName("id")
            private int id;

            @SerializedName("flag")
            private String flag;

            @SerializedName("bucket_list")
            private List<String> buckets;
        }

        @Override
        public void run() {
            try {
                synchronized (mLock) {
                    if (!mUnidentifiedBuckets.isEmpty() && MyVars.status.platformStatus) {
                        Reply reply = NetHelper.getInstance().checkStackOrSingle(
                                mUnidentifiedBuckets.get(0)).execute().body();
                        if (reply != null && reply.getCode() == 200) {
                            playSound();
                            StackInfo info = new Gson().fromJson(reply.getContent(), StackInfo.class);
                            if ("0".equals(info.flag) || "2".equals(info.flag)) {
                                mBulkBuckets.add(mUnidentifiedBuckets.remove(0));
                            } else {
                                if (mStack1Buckets.isEmpty()) {
                                    mStack1Id = info.id;
                                    mStack1Buckets.addAll(info.buckets);
                                    mUnidentifiedBuckets.removeAll(mStack1Buckets);
                                } else if (mStack2Buckets.isEmpty()) {
                                    mStack2Id = info.id;
                                    mStack2Buckets.addAll(info.buckets);
                                    mUnidentifiedBuckets.removeAll(mStack2Buckets);
                                } else {
                                    // TODO: 2018.10.18 第三垛提示
                                }
                            }
                            Message.obtain(mHandler, MSG_UPDATE_STACKS).sendToTarget();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class DeliveryBillQueryTask implements Runnable {

        private class Form {

            @SerializedName("form_id")
            private String id;

            @SerializedName("client_name")
            private String client;

            @SerializedName("driver_name")
            private String driver;

            @SerializedName("product_list")
            private List<Product> products;
        }

        private class Product {

            @SerializedName("code")
            private int code;

            @SerializedName("quantity")
            private int quantity;
        }

        @Override
        public void run() {
            try {
                Reply reply = NetHelper.getInstance().queryDeliveryBills().execute().body();
                if (reply != null && reply.getCode() == 200) {
                    List<Form> forms = new Gson().fromJson(reply.getContent(),
                            new TypeToken<List<Form>>(){}.getType());
                    if (forms == null) return;
                    for (Form form : forms) {
                        if (!mBillsMap.containsKey(form.id) && form.id.startsWith(MyVars.config.getCompanySymbol())) {
                            DeliveryBill bill = new DeliveryBill(form.id, form.client, form.driver);
                            for (Product product : form.products) {
                                bill.addGoods(product.code, product.quantity);
                            }
                            mBills.add(0, bill);
                            mBillsMap.put(bill.getBillID(), bill);
                        }
                    }
                    Iterator<Map.Entry<String, DeliveryBill>> i = mBillsMap.entrySet().iterator();
                    while (i.hasNext()) {
                        Map.Entry<String, DeliveryBill> e = i.next();
                        if (!e.getValue().isFromCard()) {
                            boolean isFormsContained = false;
                            for (Form form : forms) {
                                if (form.id.equals(e.getKey())) {
                                    isFormsContained = true;
                                }
                            }
                            if (!isFormsContained) {
                                mBills.remove(e.getValue());
                                i.remove();
                            }
                        }
                    }
                    Message.obtain(mHandler, MSG_UPDATE_BILLS).sendToTarget();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
