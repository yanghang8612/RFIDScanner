package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.DeliveryDetailActivity;
import com.casc.rfidscanner.activity.MainActivity;
import com.casc.rfidscanner.adapter.DeliveryBillAdapter;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.suke.widget.SwitchButton;

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
 * 成品水出库Fragment
 */
public class R6Fragment extends BaseFragment implements InsHandler {

    private enum WorkStatus {
        IS_IDLE, IS_WORKING, READY_BACKING, IS_BACKING, IS_READING
    }

    private static final String TAG = R6Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_DELIVERY_OR_BACK = 0;
    private static final int MSG_UPDATE_HINT = 1;
    private static final int MSG_UPDATE_COUNT = 2;
    private static final int MSG_IS_NORMAL = 3;
    private static final int MSG_IS_WORKING = 4;
    private static final int MSG_READY_BACKING = 5;
    private static final int MSG_IS_BACKING = 6;

    @BindView(R.id.sbtn_delivery_back) SwitchButton mDeliveryBackSbtn;
    @BindView(R.id.tv_r6_work_status) TextView mWorkStatusTv;
    @BindView(R.id.tv_r6_temp_count) TextView mTempCountTv;
    @BindView(R.id.tv_r6_uploaded_bill_count) TextView mUploadedBillCountTv;
    @BindView(R.id.tv_r6_stored_bill_count) TextView mStoredBillCountTv;

    @BindView(R.id.rv_delivery_bill) RecyclerView mBillView;
    @BindView(R.id.rv_r6_hint_list) RecyclerView mHintRv;

    // 出库单列表
    private List<DeliveryBill> mBills = new ArrayList<>();

    // 出库单map，用于根据出货单EPC获取出库单实例
    private Map<String, DeliveryBill> mBillsMap = new HashMap<>();

    // 提示消息列表
    private List<Hint> mHints = new ArrayList<>();

    // 出库单列表适配器
    private DeliveryBillAdapter mBillAdapter;

    // 提示消息列表适配器
    private HintAdapter mHintAdapter;

    // 当前正在出库的出库单
    private DeliveryBill mCurBill;

    // 当前正在出库的桶列表（存储桶身EPC信息）
    private List<String> mTempEPCs = new ArrayList<>();

    // 历史出库记录的缓存，用于排除偶然扫描数据
    private Set<String> mCache = new HashSet<>();

    // 读取到的EPC计数以及检测到的空白期间隔
    private int mBucketReadCount, mCardReadCount, mBlankCount;

    // 小推车被识别开始时间
    private long mStartTime;

    // 工作状态
    private WorkStatus mStatus = WorkStatus.IS_IDLE;

    // 退库取消标识符，同时出库提示标识符，卡EPC编码解析错误标识符
    private boolean isFromCancel, isMultiDeliveryMentioned, isNoneMatchedBillWhenBacking,
            isCardEPCCodeError, isCardNotReadUserMemory;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillFinishedMessage message) {
        DeliveryBill bill = MyVars.deliveryBillToShow;
        writeHint(bill.getCardID() + "出库完成");
        // 异步上传平台
        writeHint(bill.getCardID() + "上报平台");
        final MessageDelivery delivery =
                new MessageDelivery(
                        TextUtils.isEmpty(bill.getBillID()) ? "0000000000" : bill.getBillID(),
                        (char) (TextUtils.isEmpty(bill.getBillID()) ? 2 : bill.checkBill() ?  0 : 1),
                        message.dealer,
                        message.driver);
        for (Bucket bucket : bill.getBuckets()) {
            delivery.addBucket(bucket.getTime() / 1000, CommonUtils.bytesToHex(bucket.getEpc()));
        }
        NetHelper.getInstance().uploadDeliveryMessage(delivery).enqueue(new Callback<Reply>() {
            @Override
            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                Reply body = response.body();
                if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                    MyVars.cache.storeDeliveryBill(delivery);
                    EventBus.getDefault().post(new BillStoredMessage());
                } else {
                    EventBus.getDefault().post(new BillUploadedMessage(false));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                MyVars.cache.storeDeliveryBill(delivery);
                EventBus.getDefault().post(new BillStoredMessage());
            }
        });
        mBills.remove(bill);
        mBillsMap.remove(CommonUtils.bytesToHex(bill.getCard()));
        mBillAdapter.notifyDataSetChanged();
        if (mBills.isEmpty()) {
            mDeliveryBackSbtn.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillStoredMessage message) {
        writeHint(MyVars.deliveryBillToShow.getCardID() + "上报失败，已转储");
        increaseCount(mStoredBillCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        if (mBills.isEmpty()) {
            Log.i(TAG, "BILL_UPDATED");
            mDeliveryBackSbtn.setEnabled(false);
        } else if (mStatus == WorkStatus.IS_IDLE) {
            mDeliveryBackSbtn.setEnabled(true);
        }
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
        mDeliveryBackSbtn.setEnabled(false);
        mDeliveryBackSbtn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                if (isChecked) { // 退库开始只能通过手动
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_READY_BACKING));
                    SpeechSynthesizer.getInstance().speak("退库开始");
                } else if (isFromCancel) {
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_NORMAL));
                    SpeechSynthesizer.getInstance().speak("退库取消");
                }
            }
        });
        mHintAdapter = new HintAdapter(mHints);
        mBillAdapter = new DeliveryBillAdapter(mBills);
        mBillAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (ActivityCollector.getTopActivity() instanceof MainActivity) {
                    MyVars.deliveryBillToShow = mBills.get(position);
                    DeliveryDetailActivity.actionStart(getContext());
                }
            }
        });

        mHintRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mHintRv.setAdapter(mHintAdapter);
        mBillView.setLayoutManager(new LinearLayoutManager(getContext()));
        mBillView.setAdapter(mBillAdapter);
        mStoredBillCountTv.setText(String.valueOf(MyVars.cache.getStoredDeliveryBillCount()));
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new BillNoOperationCheckTask(), 0, 1, TimeUnit.SECONDS);
        //mBills.add(new DeliveryBill(CommonUtils.hexToBytes("314159510100030000000000"), CommonUtils.hexToBytes("3A0202A800FA000FA000FA000FA000FA0000000000000000")));
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6;
    }

    @Override
    public void sensorSignal(boolean isHigh) {
        
    }

    @Override
    public void dealIns(byte[] ins) {
        long enterTime = System.nanoTime();
        int command = ins[2] & 0xFF;
        switch (command) {
            case 0x22: { // 轮询成功的处理流程
                mBlankCount = 0;
                int pl = ((ins[3] & 0xFF) << 8) + (ins[4] & 0xFF);
                byte[] epc = Arrays.copyOfRange(ins, 8, pl + 3);
                String epcStr = CommonUtils.bytesToHex(epc);
                switch (CommonUtils.validEPC(epc)) {
                    case NONE: // 检测到未注册标签，是否提示
                        break;
                    case BUCKET:
                        // 不管出库或退库，扫到桶则Disable退库Button
                        mBucketReadCount++;
                        if (mBucketReadCount + mCardReadCount >= MyParams.SINGLE_CART_MIN_SCANNED_COUNT) {
                            if (mStatus == WorkStatus.IS_IDLE) { // 正常出库，修改工作状态的提示
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_WORKING));
                            }
                            if (mStatus == WorkStatus.READY_BACKING) { // 准备退库，修改工作状态的提示
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_BACKING));
                            }
                            if (!mTempEPCs.contains(epcStr)) { // 扫到的EPC均判重后加入temp列表里并更新显示的扫描数量
                                mTempEPCs.add(epcStr);
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_COUNT));
                            }
                        }
                        break;
                    case CARD_DELIVERY:
                        // 不管出库或退库，扫到卡则Disable退库Button
                        mCardReadCount++;
                        if (mStatus == WorkStatus.IS_IDLE
                                && epc[7] == 0x00
                                && !mBillsMap.containsKey(epcStr)
                                && mCardReadCount >= MyParams.DELIVERY_CARD_SCANNED_COUNT
                                && mBucketReadCount == 0) {
                            // 尝试读取User Memory
                            // 下发Mask指令
                            MyVars.getReader().sendCommand(InsHelper.getEPCSelectParameter(epc), MyParams.SELECT_MAX_TRY_COUNT);
                            // 下发TID读取指令
                            MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                                    CommonUtils.hexToBytes("00000000"),
                                    InsHelper.MemBankType.UserMem,
                                    MyParams.USER_MEMORY_START_INDEX,
                                    MyParams.USER_MEMORY_LENGTH), MyParams.READ_USER_MEMORY_MAX_TRY_COUNT);
                            mCardReadCount = 0;
                        }
                        else if (mBucketReadCount + mCardReadCount >= MyParams.SINGLE_CART_MIN_SCANNED_COUNT) {
                            if (mStatus == WorkStatus.IS_IDLE) { // 正常出库，修改工作状态的提示
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_WORKING));
                            }
                            if (mStatus == WorkStatus.READY_BACKING) { // 准备退库，修改工作状态的提示
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_BACKING));
                            }
                            if (mCurBill == null || !mCurBill.isHighlight()) { // 当前的Bill为空则查找或生成该单
                                if (mBillsMap.containsKey(epcStr)) {
                                    mCurBill = mBillsMap.get(epcStr);
                                    mBills.remove(mCurBill);
                                    mBills.add(0, mCurBill);
                                    SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum() + (mStatus != WorkStatus.IS_BACKING ? "出库中" : "退库中"));
                                } else {
                                    if (mStatus == WorkStatus.IS_BACKING) {
                                        isNoneMatchedBillWhenBacking = true;
                                    } else if (epc[7] == 0x01) {
                                        mCurBill = new DeliveryBill(epc);
                                        mBills.add(0, mCurBill);
                                        mBillsMap.put(CommonUtils.bytesToHex(epc), mCurBill);
                                        SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum() + (mStatus != WorkStatus.IS_BACKING ? "出库中" : "退库中"));
                                    } else if (isCardNotReadUserMemory) {
                                        isCardNotReadUserMemory = true;
                                        SpeechSynthesizer.getInstance().speak("出库中发现未刷出库卡");
                                    }
                                }
                                if (mCurBill != null) {
                                    mCurBill.setUpdatedTime(System.currentTimeMillis());
                                    mCurBill.setHighlight(true);
                                    EventBus.getDefault().post(new BillUpdatedMessage());
                                }
                            } else if (!Arrays.equals(epc, mCurBill.getCard()) && !isMultiDeliveryMentioned) {
                                isMultiDeliveryMentioned = true;
                                SpeechSynthesizer.getInstance().speak("出库中发现两张以上出库卡");
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
            }
            case 0x39: { // 读取User Memory成功
                byte[] epc = Arrays.copyOfRange(ins, 8, 6 + ins[5]);
                byte[] data = Arrays.copyOfRange(ins, 6 + ins[5], ins.length - 2);
                Log.i(TAG, "EPC: " + CommonUtils.bytesToHex(epc));
                Log.i(TAG, "DATA: " + CommonUtils.bytesToHex(data));
                if (!mBillsMap.containsKey(CommonUtils.bytesToHex(epc))) {
                    try {
                        mBills.add(0, new DeliveryBill(epc, data));
                        mBillsMap.put(CommonUtils.bytesToHex(epc), mBills.get(0));
                        SpeechSynthesizer.getInstance().speak("刷卡成功");
                        EventBus.getDefault().post(new BillUpdatedMessage());
                    } catch (Exception ignored) {
                        ignored.printStackTrace();
                        isCardEPCCodeError = true;
                    }
                    break;
                }
            }
            default: { // 命令帧执行失败的处理流程
                mAdminCardScannedCount = 0;
                int discoveryInterval =
                        (int) (Double.valueOf(
                                ConfigHelper.getParam(MyParams.S_DISCOVERY_INTERVAL)
                                        .replace("Sec", ""))
                                * 1000);
                // 在工作状态，达到了空白期间隔设定值且发现间隔大于最小时间间隔
                if ((mStatus == WorkStatus.IS_WORKING || mStatus == WorkStatus.IS_BACKING)
                        && ++mBlankCount >= Integer.valueOf(ConfigHelper.getParam(MyParams.S_BLANK_INTERVAL))
                        && System.currentTimeMillis() - mStartTime > discoveryInterval) {

                    mBlankCount = 0;

                    // 首先处理相关结果的展示信息
                    if (isNoneMatchedBillWhenBacking) {
                        SpeechSynthesizer.getInstance().speak("退库前请先出库");
                    } else if (isCardEPCCodeError) {
                        SpeechSynthesizer.getInstance().speak("解析出库卡出错");
                    } else if (mCurBill == null || !mCurBill.isHighlight()) { // 检测到有桶在出库或退库，但是没有扫到出库卡
                        if (!mTempEPCs.isEmpty()) {
                            if (mStatus == WorkStatus.IS_BACKING) {
                                writeHint("未发现出库卡，退库结束");
                                SpeechSynthesizer.getInstance().speak("未发现出库卡，退库结束");
                            } else {
                                SpeechSynthesizer.getInstance().speak("未发现出库卡");
                            }
                        }
                    } else { // 检测到出库卡，看是否扫描到桶标签
                        if (mTempEPCs.isEmpty()) {
                            if (mStatus == WorkStatus.IS_BACKING) {
                                writeHint(mCurBill.getCardID() + "退库结束(未发现桶标签)");
                                SpeechSynthesizer.getInstance().speak("未发现桶标签，退库结束");
                            } else {
                                writeHint(mCurBill.getCardID() + "空车通过");
                                SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum() + "空车通过");
                            }
                        } else {
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_DELIVERY_OR_BACK));
                        }
                    }

                    // 其次发送NORMAL消息
                    mHandler.sendMessage(Message.obtain(mHandler, MSG_IS_NORMAL));
                }
            }
        }
        long costTime = System.nanoTime() - enterTime;
        if (costTime > 1000000)
            Log.i(TAG, "Cost: " + costTime);
    }

    private void writeHint(String content) {
        mHints.add(0, new Hint(content));
        mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_HINT));
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
                case MSG_DELIVERY_OR_BACK:
                    // 添加产品到出库单中
                    int count = 0;
                    for (String epc : outer.mTempEPCs) {
                        if (outer.mStatus == WorkStatus.IS_BACKING && outer.mCurBill.removeProduct(new Bucket(epc))) { // 退库单个产品
                            //outer.mCache.add(epc);
                            count++;
                        }
                        if (outer.mStatus != WorkStatus.IS_BACKING && outer.mCurBill.addProduct(new Bucket(epc))) { // 出库单个产品
                            //outer.mCache.add(epc);
                            count++;
                        }
                    }

                    // 出库成功或退库成功提示
                    Log.i(TAG, outer.mStatus.name());
                    if (outer.mStatus == WorkStatus.IS_BACKING) {
                        outer.writeHint(
                                outer.mCurBill.getCardID() + "退库:" +
                                        outer.mTempEPCs.size() + "桶");
                        SpeechSynthesizer.getInstance().speak(
                                outer.mCurBill.getCardNum() + "退库" +
                                        CommonUtils.numToChinese(outer.mTempEPCs.size()) + "桶");
                    } else {
                        outer.writeHint(
                                outer.mCurBill.getCardID() + "出库:" +
                                        outer.mTempEPCs.size() + "桶(重复:" + (outer.mTempEPCs.size() - count) + "桶)");
                        SpeechSynthesizer.getInstance().speak(
                                outer.mCurBill.getCardNum() + "出库" +
                                        CommonUtils.numToChinese(outer.mTempEPCs.size()) + "桶");
                    }
                    break;
                case MSG_UPDATE_HINT:
                    outer.mHintAdapter.notifyDataSetChanged();
                    break;
                case MSG_UPDATE_COUNT:
                    outer.mTempCountTv.setText(String.valueOf(outer.mTempEPCs.size()));
                    break;
                case MSG_IS_NORMAL:
                    if (outer.mCurBill != null) {
                        outer.mCurBill.setHighlight(false);
                    }
                    //outer.mCurBill = null;
                    outer.mTempEPCs.clear();
                    Log.i(TAG, "MSG_NORMAL");
                    outer.mDeliveryBackSbtn.setEnabled(true);
                    outer.mDeliveryBackSbtn.setChecked(false);
                    outer.mStatus = WorkStatus.IS_IDLE;
                    outer.isMultiDeliveryMentioned = false;
                    outer.isNoneMatchedBillWhenBacking = false;
                    outer.isCardEPCCodeError = false;
                    outer.isCardNotReadUserMemory = false;
                    outer.mWorkStatusTv.setText("空闲");
                    outer.mWorkStatusTv.setBackgroundResource(R.drawable.bg_status_normal);
                    outer.mTempCountTv.setText("0");
                    outer.mBucketReadCount = outer.mCardReadCount = 0;
                    EventBus.getDefault().post(new BillUpdatedMessage());
                    break;
                case MSG_IS_WORKING:
                    outer.mStatus = WorkStatus.IS_WORKING;
                    outer.mStartTime = System.currentTimeMillis();
                    outer.isFromCancel = false;
                    outer.mDeliveryBackSbtn.setEnabled(false);
                    outer.mWorkStatusTv.setText("出库");
                    outer.mWorkStatusTv.setBackgroundResource(R.drawable.bg_status_working);
                    Log.i(TAG, "MSG_WORKING");
                    outer.mDeliveryBackSbtn.setEnabled(false);
                    break;
                case MSG_READY_BACKING:
                    outer.mStatus = WorkStatus.READY_BACKING;
                    outer.isFromCancel = true;
                    outer.mWorkStatusTv.setText("退库");
                    outer.mWorkStatusTv.setBackgroundResource(R.drawable.bg_status_backing);
                    break;
                case MSG_IS_BACKING:
                    outer.mStatus = WorkStatus.IS_BACKING;
                    outer.mStartTime = System.currentTimeMillis();
                    outer.isFromCancel = false;
                    outer.mDeliveryBackSbtn.setEnabled(false);
                    break;
            }
        }
    }

    private class BillNoOperationCheckTask implements Runnable {

        @Override
        public void run() {
            for (DeliveryBill bill : mBills) {
                if (System.currentTimeMillis() - bill.getUpdatedTime() > MyParams.BILL_NO_OPERATION_CHECK_INTERVAL) {
                    bill.setUpdatedTime(System.currentTimeMillis());
                    SpeechSynthesizer.getInstance().speak("请及时确认" + bill.getCardNum() + "出库单");
                }
            }
        }
    }
}
