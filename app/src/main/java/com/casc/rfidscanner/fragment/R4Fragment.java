package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.ProductAdapter;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.bean.Product;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.message.TagStoredMessage;
import com.casc.rfidscanner.message.TagUploadedMessage;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;

/**
 * 成品注册Fragment
 */
public class R4Fragment extends BaseFragment implements InstructionHandler {

    private static final String TAG = R4Fragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_INCREASE_SCANNED_COUNT = 0;

    @BindView(R.id.tv_scanned_count) TextView mScannedCountTv;
    @BindView(R.id.tv_uploaded_count) TextView mUploadedCountTv;
    @BindView(R.id.tv_stored_count) TextView mStoredCountTv;
    @BindView(R.id.rv_encapsulation_products) RecyclerView mEncapsulationProductsTv;

    // 已筛选桶列表
    private List<Product> mProducts = new ArrayList<>();

    // 已筛选桶列表适配器
    private ProductAdapter mAdapter;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagStoredMessage message) {
        increaseCount(mStoredCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagUploadedMessage message) {
        if (message.isFromDB)
            decreaseCount(mStoredCountTv);
        increaseCount(mUploadedCountTv);
    }

    @Override
    protected void initFragment() {
        mStoredCountTv.setText(String.valueOf(MyVars.cache.getStoredCount()));
        mAdapter = new ProductAdapter(mProducts);
        mEncapsulationProductsTv.setLayoutManager(new LinearLayoutManager(getContext()));
        mEncapsulationProductsTv.setAdapter(mAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r4;
    }

    @Override
    public void deal(byte[] ins) {
        if(D) Log.i(TAG, CommonUtils.bytesToHex(ins));
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
                            if (mProducts.size() > MyParams.PRODUCT_LIST_MAX_COUNT) mProducts.clear();
                            mProducts.add(0, new Product(epc));
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_INCREASE_SCANNED_COUNT));
                            // 下发Mash指令
                            MyVars.getReader().sendCommand(InsHelper.getEPCSelectParameter(epc), MyParams.SELECT_MAX_TRY_COUNT);
                            // 下发TID读取指令
                            MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                                    CommonUtils.hexToBytes("00000000"),
                                    InsHelper.MemBankType.TID,
                                    MyParams.TID_START_INDEX,
                                    MyParams.TID_LENGTH), MyParams.READ_TID_MAX_TRY_COUNT);
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

        private WeakReference<R4Fragment> mOuter;

        InnerHandler(R4Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R4Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_INCREASE_SCANNED_COUNT:
                    outer.increaseCount(outer.mScannedCountTv);
                    outer.mAdapter.notifyDataSetChanged();
                    outer.playSound(0, 1);
                    break;
            }
        }
    }
}
