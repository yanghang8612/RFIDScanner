package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.BucketAdapter;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.message.TagCountChangedMessage;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;

/**
 * 桶筛选Fragment
 */

public class R3Fragment extends BaseFragment implements InsHandler {

    private static final String TAG = R3Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_INCREASE_SCANNED_COUNT = 0;

    @BindView(R.id.tv_scanned_count) TextView mScannedCountTv;
    @BindView(R.id.tv_uploaded_count) TextView mUploadedCountTv;
    @BindView(R.id.tv_stored_count) TextView mStoredCountTv;
    @BindView(R.id.rv_filtrate_products) RecyclerView mFiltrateProductsTv;

    // 已筛选桶列表
    private List<Bucket> mBuckets = new ArrayList<>();

    // 已筛选桶列表适配器
    private BucketAdapter mAdapter;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagCountChangedMessage message) {
        if (Integer.valueOf(mScannedCountTv.getText().toString()) < message.scannedCount) {
            playSound();
        }
        mAdapter.notifyDataSetChanged();
        mScannedCountTv.setText(String.valueOf(message.scannedCount));
        mUploadedCountTv.setText(String.valueOf(message.uploadedCount));
        mStoredCountTv.setText(String.valueOf(message.storedCount));
    }

    @Override
    protected void initFragment() {
        mStoredCountTv.setText(String.valueOf(MyVars.cache.getStoredCount()));
        mAdapter = new BucketAdapter(mBuckets);
        mFiltrateProductsTv.setLayoutManager(new LinearLayoutManager(getContext()));
        mFiltrateProductsTv.setAdapter(mAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r3;
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
                MyParams.EPCType epcType = CommonUtils.validEPC(epc);
                switch (epcType) {
                    case NONE: // 检测到未注册标签，是否提示
                        break;
                    case BUCKET:
                        if (MyVars.cache.insert(CommonUtils.bytesToHex(epc))) {
                            if (mBuckets.size() > MyParams.PRODUCT_LIST_MAX_COUNT) {
                                mBuckets.clear();
                            }
                            mBuckets.add(0, new Bucket(epc));
//                            // 下发Mask指令
//                            MyVars.getReader().sendCommand(InsHelper.getEPCSelectParameter(epc), MyParams.SELECT_MAX_TRY_COUNT);
//                            // 下发TID读取指令
//                            MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
//                                    CommonUtils.hexToBytes("00000000"),
//                                    InsHelper.MemBankType.TID,
//                                    MyParams.TID_START_INDEX,
//                                    MyParams.TID_LENGTH), MyParams.READ_TID_MAX_TRY_COUNT);
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
            case 0x39: // 读TID成功的处理流程
                MyVars.cache.setTID(ins);
                break;
            default: // 命令帧执行失败的处理流程
                switch (ins[5] & 0xFF) {
                    case 0x15: // 轮询失败
                        mAdminCardScannedCount = 0;
                        break;
                }
        }
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R3Fragment> mOuter;

        InnerHandler(R3Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R3Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_INCREASE_SCANNED_COUNT:
                    outer.increaseCount(outer.mScannedCountTv);
                    outer.mAdapter.notifyDataSetChanged();
                    outer.playSound();
                    break;
            }
        }
    }
}
