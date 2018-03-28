package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
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
public class R2Fragment extends BaseFragment implements InstructionHandler {

    private enum WorkStatus {
        IS_IDLE, IS_WORKING
    }

    private static final String TAG = R2Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_REFLUX = 0;
    private static final int MSG_UPDATE_HINT = 1;
    private static final int MSG_UPDATE_COUNT = 2;
    private static final int MSG_IS_NORMAL = 3;
    private static final int MSG_IS_WORKING = 4;

    @BindView(R.id.tv_r2_work_status) TextView mWorkStatusTv;
    @BindView(R.id.tv_r2_temp_count) TextView mTempCountTv;
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
    private RefluxBill mCurBill;

    // 当前正在回流的桶列表（存储桶身EPC信息）
    private List<String> mTempEPCs = new ArrayList<>();

    // 历史回流记录的缓存，用于排除偶然扫描数据
    private Set<String> mCache = new HashSet<>();

    // 读取到的EPC计数以及检测到的空白期间隔
    private int mReadCount, mBlankCount;

    // 小推车被识别开始时间
    private long mStartTime;

    // 工作状态
    private WorkStatus mStatus;

    // 同时回流提示标识符，卡EPC编码解析错误标识符
    private boolean isMultiRefluxMentioned, isCardEPCCodeError;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillFinishedMessage message) {
        RefluxBill bill = MyVars.refluxBillToShow;
        writeHint(bill.getCardID() + "回流完成");
        writeHint(bill.getCardID() + "上报平台");
        final MessageReflux reflux = new MessageReflux();
        reflux.setDealer(message.dealer);
        reflux.setDriver(message.driver);
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
        mBills.remove(bill);
        mBillsMap.remove(CommonUtils.bytesToHex(bill.getCardEPC()));
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
    public void deal(byte[] ins) {
        int command = ins[2] & 0xFF;
        switch (command) {
            case 0x22: // 轮询成功的处理流程
                mBlankCount = 0;
                int pl = ((ins[3] & 0xFF) << 8) + (ins[4] & 0xFF);
                byte[] epc = Arrays.copyOfRange(ins, 8, pl + 3);
                String epcStr = CommonUtils.bytesToHex(epc);
                switch (CommonUtils.validEPC(epc)) {
                    case NONE: // 检测到未注册标签，是否提示
                        break;
                    case BUCKET:
                        mReadCount++;
                        if (mReadCount >= MyParams.SINGLE_CART_MIN_SCANNED_COUNT) {
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_WORKING));
                            if (!mTempEPCs.contains(epcStr)) { // 扫到的EPC均判重后加入temp列表里
                                mTempEPCs.add(epcStr);
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_COUNT));
                            }
                        }
                        break;
                    case CARD_REFLUX:
                        mReadCount++;
                        if (mReadCount >= MyParams.SINGLE_CART_MIN_SCANNED_COUNT) {
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_WORKING));
                            if (mCurBill == null) {
                                if (mBillsMap.containsKey(epcStr)) {
                                    mCurBill = mBillsMap.get(epcStr);
                                } else {
                                    try {
                                        mCurBill = new RefluxBill(epc);
                                    } catch (Exception e) {
                                        isCardEPCCodeError = true;
                                        return;
                                    }
                                    mBills.add(0, mCurBill);
                                    mBillsMap.put(epcStr, mCurBill);
                                }
                                mCurBill.setUpdatedTime(System.currentTimeMillis());
                                mCurBill.setHighlight(true);
                                EventBus.getDefault().post(new BillUpdatedMessage());
                                SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum() + "回收中");
                            } else if (!Arrays.equals(epc, mCurBill.getCardEPC()) && !isMultiRefluxMentioned) {
                                isMultiRefluxMentioned = true;
                                SpeechSynthesizer.getInstance().speak("回收中发现两张以上回流卡");
                            }
                        }
                        break;
                    case CARD_ADMIN:
                        mAdminCardScannedCount++;
                        if (mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                            if (mStatus == WorkStatus.IS_IDLE) {
                                sendAdminLoginMessage(CommonUtils.bytesToHex(epc));
                                ConfigActivity.actionStart(getContext());
                            } else {
                                mAdminCardScannedCount = 0;
                            }
                        }
                        break;
                }
                break;
            default: // 命令帧执行失败的处理流程，本环节只有轮询失败
                mAdminCardScannedCount = 0;
                int discoveryInterval =
                        (int) (Double.valueOf(
                                ConfigHelper.getParam(MyParams.S_DISCOVERY_INTERVAL)
                                        .replace("Sec", ""))
                                * 1000);
                // 在工作状态，达到了空白期间隔设定值且发现间隔大于最小时间间隔
                if (mStatus != WorkStatus.IS_IDLE
                        && ++mBlankCount >= Integer.valueOf(ConfigHelper.getParam(MyParams.S_BLANK_INTERVAL))
                        && System.currentTimeMillis() - mStartTime > discoveryInterval) { // 达到了空白期间隔设定值
                    if (mReadCount < MyParams.SINGLE_CART_MIN_SCANNED_COUNT) {
                        mReadCount = 0;
                    } else if (isCardEPCCodeError) {
                        SpeechSynthesizer.getInstance().speak("解析回流卡出错，请重试或联系营销人员");
                    } else if (mCurBill == null && !mTempEPCs.isEmpty()) { // 检测到有桶在回收但是没有扫到回流卡，应声音提示
                        writeHint("未发现回流卡");
                        SpeechSynthesizer.getInstance().speak("未发现回流卡");
                    } else if (mCurBill != null && mTempEPCs.size() == 0) { // EPC临时列表为空，空车通过
                        writeHint(mCurBill.getCardID() + "空车通过");
                        SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum() + "空车通过");
                    } else if (mCurBill != null && mTempEPCs.size() != 0) { // EPC临时列表不为空，正常回流
                        mHandler.sendMessage(Message.obtain(mHandler, MSG_REFLUX));
                    }
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_NORMAL));
                }
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
                case MSG_RECEIVED_FRAME_FROM_READER:
                    break;
                case MSG_REFLUX:
                    // 添加空桶到回流单中
                    int count = 0;
                    for (String epc : outer.mTempEPCs) {
                        if (outer.mCurBill.addBucket(new Bucket(epc))) {
                            count++;
                        }
                    }

                    // 回流成功提示
                    outer.writeHint(
                            outer.mCurBill.getCardID() + "回收:" +
                                    outer.mTempEPCs.size() + "桶(重复:" + (outer.mTempEPCs.size() - count) + "桶)");
                    SpeechSynthesizer.getInstance().speak(
                            outer.mCurBill.getCardNum() + "回收" +
                                    CommonUtils.numToChinese(outer.mTempEPCs.size()) + "桶");
                    break;
                case MSG_UPDATE_HINT:
                    outer.mHintAdapter.notifyDataSetChanged();
                    break;
                case MSG_UPDATE_COUNT:
                    outer.increaseCount(outer.mTempCountTv);
                    break;
                case MSG_IS_NORMAL:
                    if (outer.mCurBill != null) {
                        outer.mCurBill.setHighlight(false);
                    }
                    outer.mCurBill = null;
                    outer.mTempEPCs.clear();
                    outer.mStatus = WorkStatus.IS_IDLE;
                    outer.isMultiRefluxMentioned = false;
                    outer.isCardEPCCodeError = false;
                    outer.mWorkStatusTv.setText("空闲");
                    outer.mWorkStatusTv.setBackgroundResource(R.drawable.bg_status_normal);
                    outer.mTempCountTv.setText("0");
                    EventBus.getDefault().post(new BillUpdatedMessage());
                    break;
                case MSG_IS_WORKING:
                    outer.mStatus = WorkStatus.IS_WORKING;
                    outer.mStartTime = System.currentTimeMillis();
                    outer.mWorkStatusTv.setText("回流中");
                    outer.mWorkStatusTv.setBackgroundResource(R.drawable.bg_status_working);
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
