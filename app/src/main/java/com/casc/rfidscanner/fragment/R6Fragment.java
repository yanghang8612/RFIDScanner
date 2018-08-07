package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.DeliveryBillAdapter;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageBillBucket;
import com.casc.rfidscanner.helper.param.MessageBillComplete;
import com.casc.rfidscanner.helper.param.MessageBillDelivery;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.ResendAllBillsMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;

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

/**
 * 成品水出库Fragment
 */
public class R6Fragment extends BaseFragment implements InsHandler {

    private static final String TAG = R6Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG = 0;

    @BindView(R.id.tv_r6_title) TextView mTitleTv;
    @BindView(R.id.rv_delivery_bill) RecyclerView mBillView;
    @BindView(R.id.tv_r6_uploaded_bill_count) TextView mUploadedBillCountTv;
    @BindView(R.id.tv_r6_stored_bill_count) TextView mStoredBillCountTv;

    // 出库单列表
    private List<DeliveryBill> mBills = new ArrayList<>();

    // 出库单map，用于根据出货单EPC获取出库单实例
    private Map<String, DeliveryBill> mBillsMap = new HashMap<>();

    // 出库单列表适配器
    private DeliveryBillAdapter mBillAdapter;

    // 当前正在出库的出库单
    private DeliveryBill mCurBill;

    // 错误已提示标识
    private boolean mIsErrorNoticed;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        super.onMessageEvent(message);
        String lineName = ConfigHelper.getParam(MyParams.S_LINE_NAMME);
        if (MyVars.server.isOnline() && !TextUtils.isEmpty(lineName))
            mTitleTv.setText("出库—" + lineName);
        else
            mTitleTv.setText(LinkType.getType().comment);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ResendAllBillsMessage message) {
        //NetHelper.getInstance().reportHeartbeat();
        for (DeliveryBill bill : mBills)
            NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(bill));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillStoredMessage message) {
        increaseCount(mStoredBillCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUploadedMessage message) {
        if (message.isFromDB) {
            decreaseCount(mStoredBillCountTv);
        }
        increaseCount(mUploadedBillCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUpdatedMessage message) {
        mBillAdapter.showBill(mCurBill);
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.VISIBLE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mBillAdapter = new DeliveryBillAdapter(getContext());
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
                        if (!MyVars.server.isOnline()) {
                            R6Fragment.this.showToast("监控APP尚未启动");
                        } else {
                            new MaterialDialog.Builder(mContext)
                                    .title("提示信息")
                                    .content("出库货物：" + mCurBill.getDeliveryCount() + "（桶），确认出库吗？")
                                    .positiveText("确认")
                                    .positiveColorRes(R.color.white)
                                    .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                                    .negativeText("取消")
                                    .negativeColorRes(R.color.gray)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            NetHelper.getInstance().reportBillComplete(
                                                    new MessageBillComplete(mCurBill.getCardID()));
                                            mBills.remove(mCurBill);
                                            mBillsMap.remove(mCurBill.getCardStr());
                                            mCurBill = mBills.isEmpty() ? null : mBills.get(0);
                                            EventBus.getDefault().post(new BillUpdatedMessage());
                                            dialog.dismiss();
                                        }
                                    })
                                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                            break;
                        }
                }
                adapter.notifyDataSetChanged();
            }
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mBillView.setLayoutManager(layoutManager);
        mBillView.setAdapter(mBillAdapter);
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new BillNoOperationCheckTask(), 0, 1, TimeUnit.SECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6;
    }

    @Override
    public void sensorSignal(boolean isHigh) {}

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
                                                new MessageBillBucket(mCurBill.getCardID(), CommonUtils.bytesToHex(epc), true));
                                    }
                                } else {
                                    if (mCurBill.addBucket(epcStr)) {
                                        playSound();
                                        EventBus.getDefault().post(new BillUpdatedMessage());
                                        NetHelper.getInstance().reportBillBucket(
                                                new MessageBillBucket(mCurBill.getCardID(), CommonUtils.bytesToHex(epc), false));
                                    }
                                }
                            }
                            break;
                        case CARD_DELIVERY:
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
                                    mBillsMap.put(mCurBill.getCardStr(), mCurBill);
                                    NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(mCurBill));
                                }
                                mCurBill.setUpdatedTime(System.currentTimeMillis());
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
                }
                case 0x39: { // 读取User Memory成功
                    byte[] epc = Arrays.copyOfRange(ins, 8, 6 + ins[5]);
                    byte[] data = Arrays.copyOfRange(ins, 6 + ins[5], ins.length - 2);
//                    Log.i(TAG, "EPC: " + CommonUtils.bytesToHex(epc));
//                    Log.i(TAG, "DATA: " + CommonUtils.bytesToHex(data));
                    if (!mBillsMap.containsKey(CommonUtils.bytesToHex(epc))) {
                        try {
                            mCurBill = new DeliveryBill(epc, data);
                            mBills.add(0, mCurBill);
                            mBillsMap.put(mCurBill.getCardStr(), mCurBill);
                            EventBus.getDefault().post(new BillUpdatedMessage());
                            SpeechSynthesizer.getInstance().speak("刷卡成功");
                            NetHelper.getInstance().reportBillDelivery(new MessageBillDelivery(mCurBill));
                        } catch (Exception ignored) {
                            ignored.printStackTrace();
                        }
                    }
                    break;
                }
                default: { // 命令帧执行失败的处理流程
                    mAdminCardScannedCount = 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            }
        }
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
