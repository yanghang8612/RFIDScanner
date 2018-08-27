package com.casc.rfidscanner.fragment;

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
import com.casc.rfidscanner.activity.BillConfirmActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.DeliveryBillAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageBillBucket;
import com.casc.rfidscanner.helper.param.MessageBillComplete;
import com.casc.rfidscanner.helper.param.MessageBillDelivery;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.ReadResultMessage;
import com.casc.rfidscanner.message.ResendAllBillsMessage;
import com.casc.rfidscanner.message.WriteResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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
public class R6Fragment extends BaseFragment {

    private static final String TAG = R6Fragment.class.getSimpleName();

    @BindView(R.id.tv_r6_title) TextView mTitleTv;
    @BindView(R.id.rv_delivery_bill) RecyclerView mBillView;
//    @BindView(R.id.tv_r6_uploaded_bill_count) TextView mUploadedBillCountTv;
//    @BindView(R.id.tv_r6_stored_bill_count) TextView mStoredBillCountTv;

    // 出库单列表
    private List<DeliveryBill> mBills = new ArrayList<>();

    // 出库单map，用于根据出货单EPC获取出库单实例
    private Map<String, DeliveryBill> mBillsMap = new HashMap<>();

    // 出库单列表适配器
    private DeliveryBillAdapter mBillAdapter;

    // 当前正在出库的出库单
    private DeliveryBill mCurBill, mReadBill;

    // 错误已提示标识
    private boolean mIsErrorNoticed;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        super.onMessageEvent(message);
        String lineName = ConfigHelper.getString(MyParams.S_LINE_NAME);
        if (MyVars.server.isOnline() && !TextUtils.isEmpty(lineName))
            mTitleTv.setText("出库—" + lineName);
        else
            mTitleTv.setText(LinkType.getType().comment);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ResendAllBillsMessage message) {
        for (DeliveryBill bill : mBills)
            NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(bill));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        if (mCurBill != null) {
            mCurBill.setUpdatedTime(System.currentTimeMillis());
        }
        mBillAdapter.showBill(mCurBill);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillFinishedMessage message) {
        // 异步上传平台
        final MessageDelivery delivery =
                new MessageDelivery(
                        TextUtils.isEmpty(mCurBill.getBillID()) ? "0000000000" : mCurBill.getBillID(),
                        (char) (TextUtils.isEmpty(mCurBill.getBillID()) ? 2 : mCurBill.checkBill() ?  0 : 1),
                        message.dealer,
                        message.driver);
        for (Bucket bucket : mCurBill.getBuckets()) {
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
        showToast("提交成功");
        NetHelper.getInstance().reportBillComplete(
                new MessageBillComplete(mCurBill.getCardID()));
        mBills.remove(mCurBill);
        mBillsMap.remove(mCurBill.getCardStr());
        mCurBill = mBills.isEmpty() ? null : mBills.get(0);
        EventBus.getDefault().post(new BillUpdatedMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
//        String content = message.isReadNone ?
//                "未发现桶标签" : "发现弱标签：" + message.weakBodyCode;
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
                            SpeechSynthesizer.getInstance().speak("出库前请先刷卡");
                        }
                    } else {
                        if (mCurBill.isBacking()) {
                            if (mCurBill.removeBucket(epcStr)) {
                                playSound();
                                EventBus.getDefault().post(new BillUpdatedMessage());
                                NetHelper.getInstance().reportBillBucket(
                                        new MessageBillBucket(mCurBill.getCardID(),
                                                CommonUtils.bytesToHex(message.epc), true));
                            }
                        } else {
                            if (mCurBill.addBucket(epcStr)) {
                                playSound();
                                EventBus.getDefault().post(new BillUpdatedMessage());
                                NetHelper.getInstance().reportBillBucket(
                                        new MessageBillBucket(mCurBill.getCardID(),
                                                CommonUtils.bytesToHex(message.epc), false));
                            }
                        }
                    }
                    break;
                case CARD_DELIVERY:
                    if (message.epc[7] == 0x00
                            && !mBillsMap.containsKey(epcStr)) {
                        Log.i(TAG, "Read no read card");
                        // 尝试读取User Memory
                        // 下发Mask指令
                        MyVars.getReader().setMask(message.epc, 2);
                        // 下发UserMemory读取指令
                        MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                                CommonUtils.hexToBytes("00000000"),
                                InsHelper.MemBankType.UserMem,
                                MyParams.USER_MEMORY_START_INDEX,
                                MyParams.USER_MEMORY_LENGTH));
                    }
                    else {
                        mIsErrorNoticed = false;
                        if (mBillsMap.containsKey(epcStr)) {
                            if (mCurBill != mBillsMap.get(epcStr)) {
                                mCurBill = mBillsMap.get(epcStr);
                                EventBus.getDefault().post(new BillUpdatedMessage());
                                SpeechSynthesizer.getInstance().speak(mCurBill.getCardNum()
                                        + (mCurBill.getBill() == null ? "开始补单" : "开始出库"));
                            }
                        } else {
                            mCurBill = new DeliveryBill(message.epc);
                            mBills.add(0, mCurBill);
                            mBillsMap.put(mCurBill.getCardStr(), mCurBill);
                            EventBus.getDefault().post(new BillUpdatedMessage());
                            SpeechSynthesizer.getInstance().speak("刷卡成功，" + mCurBill.getCardNum() + "开始补单");
                            NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(mCurBill));
                        }
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
//        byte[] epc = Arrays.copyOfRange(ins, 8, 6 + ins[5]);
//        byte[] data = Arrays.copyOfRange(ins, 6 + ins[5], ins.length - 2);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ReadResultMessage message) {
        if ((message.data[0] & 0xFF) == 0xBB && (message.data[1] & 0xFF) == 0xBB) {
            if (mReadBill != null
                    && Arrays.equals(mReadBill.getCard(), message.epc)
                    && !mBillsMap.containsKey(mReadBill.getCardStr())) { // 实际写入成功，但是软件认为失败，则重新展示该Bill
                mCurBill = mReadBill;
                mReadBill = null;
                mBills.add(0, mCurBill);
                mBillsMap.put(mCurBill.getCardStr(), mCurBill);
                EventBus.getDefault().post(new BillUpdatedMessage());
                SpeechSynthesizer.getInstance().speak("刷卡成功，" + mCurBill.getCardNum() + "开始出库");
                NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(mCurBill));
            } else {
                showToast("该卡已提货，请联系销售部");
            }
        } else {
            mReadBill = new DeliveryBill(message.epc, message.data);
            // 下发UserMemory擦除指令
            MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                    CommonUtils.hexToBytes("00000000"),
                    InsHelper.MemBankType.UserMem,
                    MyParams.USER_MEMORY_START_INDEX,
                    new byte[]{(byte) 0xBB, (byte) 0xBB}));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(WriteResultMessage message) {
        if (mReadBill != null && !mBillsMap.containsKey(mReadBill.getCardStr())) {
            mCurBill = mReadBill;
            mBills.add(0, mCurBill);
            mBillsMap.put(mCurBill.getCardStr(), mCurBill);
            EventBus.getDefault().post(new BillUpdatedMessage());
            SpeechSynthesizer.getInstance().speak("刷卡成功" + mCurBill.getCardNum() + "开始出库");
            NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(mCurBill));
        }
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mBillAdapter = new DeliveryBillAdapter(mContext);
        mBillAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                switch (view.getId()) {
                    case R.id.btn_state_delivery:
                        mCurBill.setBacking(true);
                        break;
                    case R.id.btn_state_back:
                        mCurBill.setBacking(false);
                        break;
                    case R.id.btn_confirm_delivery:
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
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new BillNoOperationCheckTask(), 0, 1, TimeUnit.SECONDS);
//        mBills.add(new DeliveryBill(
//                CommonUtils.hexToBytes("314159510100030000000000"),
//                CommonUtils.hexToBytes("73820188004B000000000000000000000000000000000000")));
//        mCurBill = mBills.get(0);
//        EventBus.getDefault().post(new BillUpdatedMessage());
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6;
    }

    private class BillNoOperationCheckTask implements Runnable {

        @Override
        public void run() {
            NetHelper.getInstance().reportHeartbeat();
            for (DeliveryBill bill : mBills) {
                if (System.currentTimeMillis() - bill.getUpdatedTime() > MyParams.BILL_NO_OPERATION_CHECK_INTERVAL) {
                    bill.setUpdatedTime(System.currentTimeMillis());
                    SpeechSynthesizer.getInstance().speak("请及时确认" + bill.getCardNum() + "出库单");
                }
            }
        }
    }
}
