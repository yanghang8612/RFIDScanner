package com.casc.rfidscanner.fragment;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.BillConfirmActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.RefluxBillAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.dao.RefluxBillDao;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 空桶回流Fragment
 */
public class R2Fragment extends BaseFragment {

    private static final String TAG = R2Fragment.class.getSimpleName();

    @BindView(R.id.tv_r2_title) TextView mTitleTv;
    @BindView(R.id.rv_reflux_bill) RecyclerView mBillView;

    // 回流单列表
    private List<RefluxBill> mBills = new ArrayList<>();

    // 回流单map，用于根据出货单EPC获取回流单实例
    private Map<String, RefluxBill> mBillsMap = new HashMap<>();

    // 回流单列表适配器
    private RefluxBillAdapter mBillAdapter;

    // 当前正在回流的回流单
    private RefluxBill mCurBill;

    private RefluxBillDao mBillDao;

    // 同时回流提示标识符，卡EPC编码解析错误标识符
    private boolean mIsErrorNoticed;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        if (mCurBill != null) {
            mCurBill.setUpdatedTime(System.currentTimeMillis());
        }
        mBillAdapter.showBill(mCurBill);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillFinishedMessage message) {
        final MessageReflux reflux = new MessageReflux(message.dealer, message.driver);
        for (Bucket bucket : mCurBill.getBuckets()) {
            reflux.addBucket(bucket.getTime() / 1000, CommonUtils.bytesToHex(bucket.getEpc()));
        }
        NetHelper.getInstance().uploadRefluxMessage(reflux).enqueue(new Callback<Reply>() {
            @Override
            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                Reply body = response.body();
                if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                    MyVars.cache.storeRefluxBill(reflux);
                    EventBus.getDefault().post(new BillStoredMessage());
                }
                else
                    EventBus.getDefault().post(new BillUploadedMessage(false));
            }

            @Override
            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                MyVars.cache.storeRefluxBill(reflux);
                EventBus.getDefault().post(new BillStoredMessage());
            }
        });
        showToast("提交成功");
        mBills.remove(mCurBill);
        mBillsMap.remove(mCurBill.getCardStr());
        mBillDao.remove(mCurBill);
        mCurBill = mBills.isEmpty() ? null : mBills.get(0);
        EventBus.getDefault().post(new BillUpdatedMessage());
    }

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
                    if (mCurBill == null) {
                        if (!mIsErrorNoticed) {
                            mIsErrorNoticed = true;
                            SpeechSynthesizer.getInstance().speak("回收前请先刷卡");
                        }
                    } else {
                        if (mCurBill.addBucket(new Bucket(message.epc))){
                            playSound();
                            mBillDao.insertBucket(mCurBill.getCardStr(), epcStr);
                            EventBus.getDefault().post(new BillUpdatedMessage());
                        }
                    }
                    break;
                case CARD_REFLUX:
                    mIsErrorNoticed = false;
                    if (mBillsMap.containsKey(epcStr)) {
                        if (mCurBill != mBillsMap.get(epcStr)) {
                            mCurBill = mBillsMap.get(epcStr);
                            EventBus.getDefault().post(new BillUpdatedMessage());
                            SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum() + "开始回收");
                        }
                    } else {
                        mCurBill = new RefluxBill(message.epc);
                        mBills.add(0, mCurBill);
                        mBillsMap.put(mCurBill.getCardStr(), mCurBill);
                        mBillDao.insert(mCurBill);
                        EventBus.getDefault().post(new BillUpdatedMessage());
                        SpeechSynthesizer.getInstance().speak("刷卡成功，" + mCurBill.getCardNum() + "开始回收");
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        sendAdminLoginMessage(CommonUtils.bytesToHex(message.epc));
                        ConfigActivity.actionStart(getContext());
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

        mBillDao = new RefluxBillDao();
        if (mBillDao.rowCount() > 0) {
            mBills.addAll(mBillDao.getAllBills());
            for (RefluxBill bill : mBills) {
                mBillsMap.put(bill.getCardStr(), bill);
            }
            mCurBill = mBills.get(0);
        }

        mBillAdapter = new RefluxBillAdapter(mContext);
        mBillAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                switch (view.getId()) {
                    case R.id.btn_confirm_reflux:
                        BillConfirmActivity.actionStart(mContext);
                        break;
                }
                adapter.notifyDataSetChanged();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mBillView.setLayoutManager(layoutManager);
        mBillView.setAdapter(mBillAdapter);
        EventBus.getDefault().post(new BillUpdatedMessage());
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new BillNoOperationCheckTask(), 0, 1, TimeUnit.SECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r2;
    }

    private class BillNoOperationCheckTask implements Runnable {

        @Override
        public void run() {
            for (RefluxBill bill : mBills) {
                if (System.currentTimeMillis() - bill.getUpdatedTime() > MyParams.BILL_NO_OPERATION_CHECK_INTERVAL) {
                    bill.setUpdatedTime(System.currentTimeMillis());
                    SpeechSynthesizer.getInstance().speak("请及时确认" + bill.getCardNum() + "回收单");
                }
            }
        }
    }
}
