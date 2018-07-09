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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 成品水出库Fragment
 */
public class R6Fragment extends BaseFragment implements InsHandler {

    private static final String TAG = R6Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_HINT = 0;
    private static final int MSG_UPDATE_CARD_ID = 1;
    private static final int MSG_UPDATE_COUNT = 2;
    private static final int MSG_RESET = 3;

    @BindView(R.id.sbtn_delivery_back) SwitchButton mDeliveryBackSbtn;
    @BindView(R.id.tv_r6_delivery_card_id) TextView mDeliveryCardIDTv;
    @BindView(R.id.tv_r6_scanned_count) TextView mScannedCountTv;
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
    private DeliveryBill mPreBill, mCurBill;

    private boolean mIsErrorNoticed;

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
        if (bill == mCurBill)
            mHandler.sendMessage(Message.obtain(mHandler, MSG_RESET));
        mBills.remove(bill);
        mBillsMap.remove(CommonUtils.bytesToHex(bill.getCard()));
        mBillAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillStoredMessage message) {
        writeHint(MyVars.deliveryBillToShow.getCardID() + "上报失败，已转储");
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
        mDeliveryBackSbtn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                mHandler.sendMessage(Message.obtain(mHandler, MSG_RESET));
                if (isChecked) { // 退库开始只能通过手动
                    SpeechSynthesizer.getInstance().speak("退库模式");
                } else {
                    SpeechSynthesizer.getInstance().speak("出库模式");
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
        try {
            int command = ins[2] & 0xFF;
            switch (command) {
                case 0x22: { // 轮询成功的处理流程
                    int pl = ((ins[3] & 0xFF) << 8) + (ins[4] & 0xFF);
                    byte[] epc = Arrays.copyOfRange(ins, 8, pl + 3);
                    String epcStr = CommonUtils.bytesToHex(epc);
                    switch (CommonUtils.validEPC(epc)) {
                        case NONE: // 检测到未注册标签，是否提示
                            break;
                        case BUCKET:
                            // 不管出库或退库，扫到桶则Disable退库Button
                            if (mCurBill == null) {
                                if (!mIsErrorNoticed) {
                                    mIsErrorNoticed = true;
                                    SpeechSynthesizer.getInstance().speak(
                                            (mDeliveryBackSbtn.isChecked() ? "退库" : "出库") + "前请先刷卡");
                                }
                            } else {
                                if (mDeliveryBackSbtn.isChecked()) {
                                    if (mCurBill.removeProduct(new Bucket(epc))){
                                        playSound();
                                        mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_COUNT));
                                        EventBus.getDefault().post(new BillUpdatedMessage());
                                    }
                                } else {
                                    if (mCurBill.addProduct(new Bucket(epc))){
                                        playSound();
                                        mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_COUNT));
                                        EventBus.getDefault().post(new BillUpdatedMessage());
                                    }
                                }
                            }
                            break;
                        case CARD_DELIVERY:
                            // 不管出库或退库，扫到卡则Disable退库Button
                            if (epc[7] == 0x00
                                    && !mBillsMap.containsKey(epcStr)) {
                                // 尝试读取User Memory
                                // 下发Mask指令
                                MyVars.getReader().setMask(epc);
                                // 下发TID读取指令
                                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                                        CommonUtils.hexToBytes("00000000"),
                                        InsHelper.MemBankType.UserMem,
                                        MyParams.USER_MEMORY_START_INDEX,
                                        MyParams.USER_MEMORY_LENGTH), MyParams.READ_USER_MEMORY_MAX_TRY_COUNT);
                            }
                            else {
                                mIsErrorNoticed = false;
                                if (mBillsMap.containsKey(epcStr)) {
                                    mCurBill = mBillsMap.get(epcStr);
                                    mBills.remove(mCurBill);
                                    mBills.add(0, mCurBill);
                                } else {
                                    mCurBill = new DeliveryBill(epc);
                                    mBills.add(0, mCurBill);
                                    mBillsMap.put(CommonUtils.bytesToHex(epc), mCurBill);
                                }
                                if (mPreBill != mCurBill) {
                                    mPreBill = mCurBill;
                                    mCurBill.setUpdatedTime(System.currentTimeMillis());
                                    mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_CARD_ID));
                                    EventBus.getDefault().post(new BillUpdatedMessage());
                                }
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
                        }
                        break;
                    }
                }
                default: { // 命令帧执行失败的处理流程
                    mAdminCardScannedCount = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                case MSG_UPDATE_HINT:
                    outer.mHintAdapter.notifyDataSetChanged();
                    break;
                case MSG_UPDATE_COUNT:
                    outer.increaseCount(outer.mScannedCountTv);
                    break;
                case MSG_UPDATE_CARD_ID:
                    if (!outer.mScannedCountTv.getText().equals("0"))
                        outer.writeHint(
                                outer.mDeliveryCardIDTv.getText()
                                        + (outer.mDeliveryBackSbtn.isChecked() ? "退库" : "出库")
                                        + outer.mScannedCountTv.getText() + "桶");
                    outer.mDeliveryCardIDTv.setText(outer.mCurBill.getCardID());
                    outer.mScannedCountTv.setText("0");
                    SpeechSynthesizer.getInstance().speak(
                            outer.mCurBill.getCardNum() + (outer.mDeliveryBackSbtn.isChecked() ? "开始退库" : "开始出库"));
                    break;
                case MSG_RESET:
                    outer.mPreBill = outer.mCurBill = null;
                    outer.mDeliveryCardIDTv.setText("空闲");
                    outer.mScannedCountTv.setText("0");
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
