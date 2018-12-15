package com.casc.rfidscanner.fragment;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.TagCountChangedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * 成品下线Fragment
 */
public class R4Fragment extends BaseFragment {

    private static final String TAG = R4Fragment.class.getSimpleName();

    @BindView(R.id.ns_r4_scanned_count) NumberSwitcher mScannedCountNs;
    @BindView(R.id.ns_r4_uploaded_count) NumberSwitcher mUploadedCountNs;
    @BindView(R.id.ns_r4_stored_count) NumberSwitcher mStoredCountNs;
    @BindView(R.id.rv_r4_hint_list) RecyclerView mHintListRv;

    // 提示信息列表
    private List<Hint> mHints = new ArrayList<>();

    // 提示信息列表适配器
    private HintAdapter mHintAdapter;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagCountChangedMessage message) {
        mScannedCountNs.setNumber(message.scannedCount);
        mUploadedCountNs.setNumber(message.uploadedCount);
        mStoredCountNs.setNumber(message.storedCount);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
        String content = message.isReadNone ?
                "未发现桶标签" : "发现弱标签：" + new Bucket(message.epc).getBodyCode();
        mHints.add(0, new Hint(content));
        mHintAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    if (MyVars.cache.insert(epcStr)) {
                        playSound();
                    }
//                    // 下发Mask指令
//                    MyVars.getReader().sendCommand(InsHelper.getEPCSelectParameter(epc), MyParams.SELECT_MAX_TRY_COUNT);
//                    // 下发TID读取指令
//                    MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
//                            CommonUtils.hexToBytes("00000000"),
//                            InsHelper.MemBankType.TID,
//                            MyParams.TID_START_INDEX,
//                            MyParams.TID_LENGTH), MyParams.READ_TID_MAX_TRY_COUNT);
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
        //MyVars.cache.setTID(ins);
    }

    @Override
    protected void initFragment() {
        mScannedCountNs.setNumber(0);
        mUploadedCountNs.setNumber(0);
        mStoredCountNs.setNumber((int) MyVars.cache.getStoredCount());

        mHintAdapter = new HintAdapter(mHints);
        mHintListRv.setLayoutManager(new LinearLayoutManager(mContext));
        mHintListRv.setAdapter(mHintAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r4;
    }
}
