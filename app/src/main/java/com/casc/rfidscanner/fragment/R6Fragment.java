package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.BillConfirmActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.StackDetailActivity;
import com.casc.rfidscanner.adapter.DeliveryBillAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.helper.EmptyAdapter;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MsgDelivery;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.DealerAndDriverSelectedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.StackDeletedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
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
import retrofit2.Response;

/**
 * 成品水出库Fragment
 */
public class R6Fragment extends BaseFragment {

    private static final String TAG = R6Fragment.class.getSimpleName();

    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE = 0;

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
//    @BindView(R.id.tv_r6_uploaded_bill_count) TextView mUploadedBillCountTv;
//    @BindView(R.id.tv_r6_stored_bill_count) TextView mStoredBillCountTv;

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

    private final Object mLock = new Object();

    // 错误已提示标识
    private boolean mIsErrorNoticed, mIsBacking;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

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
            Message.obtain(mHandler, MSG_UPDATE).sendToTarget();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        mBillAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DealerAndDriverSelectedMessage message) {
        // 异步上传平台
        MsgDelivery delivery = new MsgDelivery(
                mCurBill.isFromCard() ? "0000000000" : mCurBill.getBillID(),
                (char) (mCurBill.isFromCard() ? 2 : mCurBill.checkBill() ?  0 : 1),
                message.dealer,
                message.driver);
        for (Bucket bucket : mCurBill.getBuckets()) {
            delivery.addBucket(bucket.getTime(), bucket.getEpcStr(), "0");
        }
        for (String epcStr : mCurBill.getRemoves()) {
            delivery.addBucket(System.currentTimeMillis(), epcStr, "2");
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
        EventBus.getDefault().post(new BillUpdatedMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    if (mIsBacking) {
                        if (mSelectedBill != null && mSelectedBill.removeBucket(epcStr)) {
                            synchronized (mLock) {
                                mCache.remove(epcStr);
                            }
                            NetHelper.getInstance().uploadUnstackInfo(Bucket.getBodyCode(epcStr))
                                    .enqueue(new EmptyAdapter());
                            EventBus.getDefault().post(new BillUpdatedMessage());
                        }
                    } else {
                        synchronized (mLock) {
                            if (!mCache.contains(epcStr)
                                    && !mStack1Buckets.contains(epcStr) && !mStack2Buckets.contains(epcStr)
                                    && !mBulkBuckets.contains(epcStr) && !mUnidentifiedBuckets.contains(epcStr)) {
                                mUnidentifiedBuckets.add(epcStr);
                                Message.obtain(mHandler, MSG_UPDATE).sendToTarget();
                            }
                        }
                    }
                    break;
                case CARD_DELIVERY:
                    if (!mBillsMap.containsKey(DeliveryBill.getCardIDFromEPC(message.epc))) {
                        DeliveryBill bill = new DeliveryBill(message.epc);
                        mBills.add(0, bill);
                        mBillsMap.put(bill.getBillID(), bill);
                        EventBus.getDefault().post(new BillUpdatedMessage());
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        sendAdminLoginMessage(CommonUtils.bytesToHex(message.epc));
                        ConfigActivity.actionStart(mContext);
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

        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mBillsRv.setLayoutManager(layoutManager);
        mBillsRv.setAdapter(mBillAdapter);
        MyVars.fragmentExecutor.schedule(new IdentifyBucketTask(), 3, TimeUnit.SECONDS);
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new DeliveryBillQueryTask(), 3, 10, TimeUnit.SECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6;
    }

    @OnClick(R.id.tv_r6_title)
    void onTitleClicked() {
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
            StackDetailActivity.actionStart(mContext, mStack1Buckets, true);
        } else if (mStackViewToDelete == mStack2Cv) {
            StackDetailActivity.actionStart(mContext, mStack2Buckets, true);
        } else if (mStackViewToDelete == mBulkCv) {
            StackDetailActivity.actionStart(mContext, mBulkBuckets, false);
        } else {
            StackDetailActivity.actionStart(mContext, mUnidentifiedBuckets, false);
        }
        return true;
    }

    private void merge() {
        if (mSelectedStackView != null && mSelectedBillView != null) {
            synchronized (mLock) {
                List<String> buckets;
                if (mSelectedStackView == mStack1Cv) {
                    buckets = mStack1Buckets;
                } else if (mSelectedStackView == mStack2Cv) {
                    buckets = mStack2Buckets;
                } else if (mSelectedStackView == mBulkCv) {
                    buckets = mBulkBuckets;
                } else {
                    buckets = mUnidentifiedBuckets;
                }
                for (String bucket : buckets) {
                    mSelectedBill.addBucket(bucket);
                }
                EventBus.getDefault().post(new BillUpdatedMessage());
                mCache.addAll(buckets);
                mSelectedStackView.setCardBackgroundColor(mContext.getColor(R.color.snow));
                mSelectedStackView = null;
                mSelectedBillView.setCardBackgroundColor(mContext.getColor(R.color.snow));
                mSelectedBillView = null;
                mSelectedBill = null;
                buckets.clear();
                Message.obtain(mHandler, MSG_UPDATE).sendToTarget();
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
                case MSG_UPDATE:
                    outer.mStack1Cv.setVisibility(outer.mStack1Buckets.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    outer.mStack1CountNs.setNumber(outer.mStack1Buckets.size());
                    outer.mStack2Cv.setVisibility(outer.mStack2Buckets.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    outer.mStack2CountNs.setNumber(outer.mStack2Buckets.size());
                    outer.mBulkCv.setVisibility(outer.mBulkBuckets.isEmpty() ? View.INVISIBLE : View.VISIBLE);
                    outer.mBulkCountNs.setNumber(outer.mBulkBuckets.size());
                    outer.mScannedCountNs.setNumber(outer.mUnidentifiedBuckets.size());
                    break;
            }
        }
    }

    private class IdentifyBucketTask implements Runnable {

        private class StackInfo {

            private String flag;

            private int number;

            private List<String> bucketepcinfo;

            public String getFlag() {
                return flag;
            }

            public List<String> getBuckets() {
                return bucketepcinfo;
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    synchronized (mLock) {
                        if (!mUnidentifiedBuckets.isEmpty() && MyVars.status.platformStatus) {
                            Response<Reply> response = NetHelper.getInstance()
                                    .checkStackOrSingle(mUnidentifiedBuckets.get(0)).execute();
                            Reply reply = response.body();
                            if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                                playSound();
                                StackInfo info = new Gson().fromJson(reply.getContent(), StackInfo.class);
                                if ("0".equals(info.getFlag()) || "2".equals(info.getFlag())) {
                                    mBulkBuckets.add(mUnidentifiedBuckets.remove(0));
                                } else {
                                    if (mStack1Buckets.isEmpty()) {
                                        mStack1Buckets.addAll(info.getBuckets());
                                        mUnidentifiedBuckets.removeAll(mStack1Buckets);
                                    } else if (mStack2Buckets.isEmpty()) {
                                        mStack2Buckets.addAll(info.getBuckets());
                                        mUnidentifiedBuckets.removeAll(mStack2Buckets);
                                    } else {
                                        // TODO: 2018.10.18 第三垛提示
                                    }
                                }
                                Message.obtain(mHandler, MSG_UPDATE).sendToTarget();
                            }
                        }
                    }
                    Thread.sleep(10);
                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class DeliveryBillQueryTask implements Runnable {

        private class Form {

            private String formid;

            private String dealer;

            private String driver;

            private class ProductInfo {

                private int productcode;

                private int productquantity;

                public int getCode() {
                    return productcode;
                }

                public int getQuantity() {
                    return productquantity;
                }
            }

            private List<ProductInfo> productinfo;

            public String getFormid() {
                return formid;
            }

            public String getDealer() {
                return dealer;
            }

            public String getDriver() {
                return driver;
            }

            public List<ProductInfo> getProductinfo() {
                return productinfo == null ? new ArrayList<ProductInfo>() : productinfo;
            }
        }

        @Override
        public void run() {
            try {
                Response<Reply> response = NetHelper.getInstance().queryDeliveryBill().execute();
                Reply reply = response.body();
                if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                    List<Form> forms = new Gson().fromJson(reply.getContent(),
                            new TypeToken<List<Form>>(){}.getType());
                    for (Form form : forms) {
                        if (!mBillsMap.containsKey(form.getFormid())) {
                            DeliveryBill bill = new DeliveryBill(form.getFormid(), form.getDealer(), form.getDriver());
                            for (Form.ProductInfo productInfo : form.getProductinfo()) {
                                bill.addGoods(productInfo.getCode(), productInfo.getQuantity());
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
                                if (form.getFormid().equals(e.getKey())) {
                                    isFormsContained = true;
                                }
                            }
                            if (!isFormsContained) {
                                mBills.remove(e.getValue());
                                i.remove();
                            }
                        }
                    }
                    EventBus.getDefault().post(new BillUpdatedMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
