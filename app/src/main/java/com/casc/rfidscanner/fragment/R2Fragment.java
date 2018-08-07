package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.MainActivity;
import com.casc.rfidscanner.activity.RefluxDetailActivity;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.adapter.RefluxBillAdapter;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 空桶回流Fragment
 */
public class R2Fragment extends BaseFragment implements InsHandler {

    private static final String TAG = R2Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_HINT = 0;
    private static final int MSG_UPDATE_CARD_ID = 1;
    private static final int MSG_UPDATE_COUNT = 2;
    private static final int MSG_RESET = 3;

    @BindView(R.id.tv_r2_title) TextView mTitleTv;
    @BindView(R.id.tv_r2_reflux_card_id) TextView mRefluxCardIDTv;
    @BindView(R.id.tv_r2_scanned_count) TextView mScannedCountTv;
    @BindView(R.id.tv_r2_uploaded_bill_count) TextView mUploadedBillCountTv;
    @BindView(R.id.tv_r2_stored_bill_count) TextView mStoredBillCountTv;

    @BindView(R.id.rv_reflux_bill) RecyclerView mBillView;
    @BindView(R.id.rv_r2_hint_list) RecyclerView mHintRv;

    // 回流单列表
    private List<RefluxBill> mBills = new ArrayList<>();

    // 回流单map，用于根据出货单EPC获取回流单实例
    private Map<String, RefluxBill> mBillsMap = new HashMap<>();

    // 提示消息列表
    private List<Hint> mHints = new ArrayList<>();

    // 回流单列表适配器
    private RefluxBillAdapter mBillAdapter;

    // 提示消息列表适配器
    private HintAdapter mHintAdapter;

    // 当前正在回流的回流单
    private RefluxBill mPreBill, mCurBill;

    // 历史回流记录的缓存，用于排除偶然扫描数据
    private Set<String> mCache = new HashSet<>();

    // 同时回流提示标识符，卡EPC编码解析错误标识符
    private boolean mIsErrorNoticed;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        super.onMessageEvent(message);
        if (!TextUtils.isEmpty(MyVars.server.getLineName()))
            mTitleTv.setText(MyVars.server.getLineName());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillFinishedMessage message) {
        RefluxBill bill = MyVars.refluxBillToShow;
        writeHint(bill.getCardID() + "回流完成");
        writeHint(bill.getCardID() + "上报平台");
        final MessageReflux reflux = new MessageReflux(message.dealer, message.driver);
        for (Bucket bucket : bill.getBuckets()) {
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
        if (bill == mCurBill)
            mHandler.sendMessage(Message.obtain(mHandler, MSG_RESET));
        mBills.remove(bill);
        mBillsMap.remove(bill.getCardStr());
        mBillAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillStoredMessage message) {
        writeHint(MyVars.refluxBillToShow.getCardID() + "上报失败，已转储");
        increaseCount(mStoredBillCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        mBillAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUploadedMessage message) {
        if (message.isFromDB) {
            decreaseCount(mStoredBillCountTv);
        }
        increaseCount(mUploadedBillCountTv);
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.VISIBLE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mHintAdapter = new HintAdapter(mHints);
        mBillAdapter = new RefluxBillAdapter(mBills);
        mBillAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (ActivityCollector.getTopActivity() instanceof MainActivity) {
                    MyVars.refluxBillToShow = mBills.get(position);
                    RefluxDetailActivity.actionStart(getContext());
                }
            }
        });

        mHintRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mHintRv.setAdapter(mHintAdapter);
        mBillView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBillView.setAdapter(mBillAdapter);
        mStoredBillCountTv.setText(String.valueOf(MyVars.cache.getStoredRefluxBill()));
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new BillNoOperationCheckTask(), 0, 1, TimeUnit.SECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r2;
    }

    @Override
    public void sensorSignal(boolean isHigh) {

    }

    @Override
    public void dealIns(byte[] ins) {
        int command = ins[2] & 0xFF;
        switch (command) {
            case 0x22: // 轮询成功的处理流程
                int pl = ((ins[3] & 0xFF) << 8) + (ins[4] & 0xFF);
                byte[] epc = Arrays.copyOfRange(ins, 8, pl + 3);
                String epcStr = CommonUtils.bytesToHex(epc);
                switch (CommonUtils.validEPC(epc)) {
                    case NONE: // 检测到未注册标签，是否提示
                        break;
                    case BUCKET:
                        if (mCurBill == null) {
                            if (!mIsErrorNoticed) {
                                mIsErrorNoticed = true;
                                SpeechSynthesizer.getInstance().speak("回流前请先刷卡");
                            }
                        } else {
                            if (mCurBill.addBucket(new Bucket(epc))){
                                playSound();
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_COUNT));
                                EventBus.getDefault().post(new BillUpdatedMessage());
                            }
                        }
                        break;
                    case CARD_REFLUX:
                        mIsErrorNoticed = false;
                        if (mBillsMap.containsKey(epcStr)) {
                            mCurBill = mBillsMap.get(epcStr);
                            mBills.remove(mCurBill);
                            mBills.add(0, mCurBill);
                        } else {
                            mCurBill = new RefluxBill(epc);
                            mBills.add(0, mCurBill);
                            mBillsMap.put(CommonUtils.bytesToHex(epc), mCurBill);
                        }
                        if (mPreBill != mCurBill) {
                            mPreBill = mCurBill;
                            mCurBill.setUpdatedTime(System.currentTimeMillis());
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_CARD_ID));
                            EventBus.getDefault().post(new BillUpdatedMessage());
                        }
                        break;
                    case CARD_ADMIN:
                        mAdminCardScannedCount++;
                        if (mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                            sendAdminLoginMessage(CommonUtils.bytesToHex(epc));
                            ConfigActivity.actionStart(getContext());
                        }
                        break;
                }
                break;
            default: // 命令帧执行失败的处理流程，本环节只有轮询失败
                mAdminCardScannedCount = 0;
        }
    }

    private void writeHint(String content) {
        mHints.add(0, new Hint(content));
        mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_HINT));
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R2Fragment> mOuter;

        InnerHandler(R2Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R2Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_UPDATE_HINT:
                    outer.mHintAdapter.notifyDataSetChanged();
                    break;
                case MSG_UPDATE_COUNT:
                    outer.increaseCount(outer.mScannedCountTv);
                    break;
                case MSG_UPDATE_CARD_ID:
                    if (!outer.mScannedCountTv.getText().equals("0"))
                        outer.writeHint(
                                outer.mRefluxCardIDTv.getText() + "回收" + outer.mScannedCountTv.getText() + "桶");
                    outer.mRefluxCardIDTv.setText(outer.mCurBill.getCardID());
                    outer.mScannedCountTv.setText("0");
                    SpeechSynthesizer.getInstance().speak(
                            outer.mCurBill.getCardNum() + "开始回收");
                    break;
                case MSG_RESET:
                    outer.mPreBill = outer.mCurBill = null;
                    outer.mRefluxCardIDTv.setText("空闲");
                    outer.mScannedCountTv.setText("0");
                    break;
            }
        }
    }

    private class BillNoOperationCheckTask implements Runnable {

        @Override
        public void run() {
            for (RefluxBill bill : mBills) {
                if (System.currentTimeMillis() - bill.getUpdatedTime() > MyParams.BILL_NO_OPERATION_CHECK_INTERVAL) {
                    bill.setUpdatedTime(System.currentTimeMillis());
                    writeHint("请及时确认" + bill.getCardID());
                    SpeechSynthesizer.getInstance().speak("请及时确认" + bill.getCardNum() + "回收单");
                }
            }
        }
    }
}
