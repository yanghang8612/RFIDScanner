package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.dao.MessageDao;
import com.casc.rfidscanner.helper.DBHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageStack;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.TagCountChangedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 成品打垛Fragment
 */
public class R5Fragment extends BaseFragment {

    private static final String TAG = R5Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_COMPLETE = 0;

    @BindView(R.id.ns_r5_scanned_count) NumberSwitcher mScannedCountNs;
    @BindView(R.id.ns_r5_stack_count) NumberSwitcher mStackCountNs;
    @BindView(R.id.ll_stack_buckets) LinearLayout mStackBucketsLl;
    @BindView(R.id.tv_stack_buckets) TextView mStackBucketsTv;
    @BindView(R.id.ll_bulk_buckets) LinearLayout mBulkBucketsLl;
    @BindView(R.id.tv_bulk_buckets) TextView mBulkBucketsTv;

    private enum WorkStatus {
        IS_IDLE, IS_STACK, IS_BULK
    }

    private WorkStatus mStatus;

    private Map<String, Integer> mBucketsToStack = new LinkedHashMap<>();

    private Map<String, Integer> mBucketsToRemove = new LinkedHashMap<>();

    private MessageDao mDao = new MessageDao(DBHelper.TABLE_NAME_STACK_DETAIL);

    private final Object mLock = new Object();

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
//        String content = message.isReadNone ?
//                "未发现桶标签" : "发现弱标签：" + new Bucket(message.epc).getBodyCode();
//        mHints.add(0, new Hint(content));
//        mHintAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    synchronized (mLock) {
                        switch (mStatus) {
                            case IS_IDLE:
                                if (!mBucketsToStack.containsKey(epcStr)) {
                                    playSound();
                                    mScannedCountNs.increaseNumber();
                                    mBucketsToStack.put(epcStr, 1);
                                } else {
                                    mBucketsToStack.put(epcStr, mBucketsToStack.get(epcStr) + 1);
                                }
                                break;
                            case IS_STACK:
                            case IS_BULK:
                                if (!mBucketsToRemove.containsKey(epcStr)) {
                                    playSound();
                                    mScannedCountNs.increaseNumber();
                                    mBucketsToRemove.put(epcStr, 1);
                                } else {
                                    mBucketsToRemove.put(epcStr, mBucketsToRemove.get(epcStr) + 1);
                                }
                                break;
                        }
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
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mStatus = WorkStatus.IS_IDLE;
        mScannedCountNs.setNumber(mBucketsToStack.size());
        mStackCountNs.setNumber(0);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r5;
    }

    @OnClick(R.id.ll_stack_buckets)
    void onStackBucketsLinearLayoutClicked() {
        synchronized (mLock) {
            mStatus = WorkStatus.IS_STACK;
            mStackBucketsLl.setEnabled(false);
            mStackBucketsLl.getBackground().setTint(mContext.getColor(R.color.red));
            mStackBucketsTv.setText("打垛中...");
            mBulkBucketsLl.setEnabled(false);
            mScannedCountNs.setNumber(0);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_COMPLETE), 20000);
        }

    }

    @OnClick(R.id.ll_bulk_buckets)
    void onBulkBucketsLinearLayoutClicked() {
        synchronized (mLock) {
            mStatus = WorkStatus.IS_BULK;
            mStackBucketsLl.setEnabled(false);
            mBulkBucketsLl.setEnabled(false);
            mBulkBucketsLl.getBackground().setTint(mContext.getColor(R.color.red));
            mBulkBucketsTv.setText("打垛中...");
            mScannedCountNs.setNumber(0);
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_COMPLETE), 20000);
        }

    }

    private static class InnerHandler extends Handler {

        private WeakReference<R5Fragment> mOuter;

        InnerHandler(R5Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R5Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_COMPLETE:
                    synchronized (outer.mLock) {
//                        outer.storeBucketsMap(outer.mBucketsToStack);
//                        outer.storeBucketsMap(outer.mBucketsToRemove);
                        for (String epcStr : outer.mBucketsToRemove.keySet()) {
                            outer.mBucketsToStack.remove(epcStr);
                        }
//                        outer.storeBucketsMap(outer.mBucketsToStack);
                        outer.uploadStackMessage(outer.mBucketsToStack,
                                outer.mStatus == WorkStatus.IS_BULK);
                        outer.mBucketsToStack = outer.mBucketsToRemove;
                        outer.mBucketsToRemove = new LinkedHashMap<>();
                        outer.mScannedCountNs.setNumber(outer.mBucketsToStack.size());
                        outer.mStackCountNs.increaseNumber();
                        outer.mStatus = WorkStatus.IS_IDLE;
                        outer.mStackBucketsLl.setEnabled(true);
                        outer.mStackBucketsLl.getBackground().setTint(outer.mContext.getColor(R.color.light_gray));
                        outer.mStackBucketsTv.setText("入整垛区");
                        outer.mBulkBucketsLl.setEnabled(true);
                        outer.mBulkBucketsLl.getBackground().setTint(outer.mContext.getColor(R.color.light_gray));
                        outer.mBulkBucketsTv.setText("入散垛区");
                    }
                    break;
            }
        }
    }

    private void storeBucketsMap(Map<String, Integer> buckets) {
        StringBuilder result = new StringBuilder("{\"size\":" + buckets.size() + ",");
        for (String epcStr : buckets.keySet()) {
            result.append("\"")
                    .append(Bucket.getBodyCode(epcStr))
                    .append("\":")
                    .append(buckets.get(epcStr)).append(",");
        }
        result.replace(result.length() - 1, result.length(), "}");
        mDao.insert(result.toString());
    }

    private void uploadStackMessage(Map<String, Integer> buckets, boolean isBulk) {
        MessageStack stack = new MessageStack(isBulk ? "0" : "1");
        for (String epcStr : buckets.keySet()) {
            stack.addBucket(epcStr);
        }
        MyVars.cache.storeStackMessage(stack);
    }
}