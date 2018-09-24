package com.casc.rfidscanner.fragment;

import android.view.View;

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

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * 桶筛选Fragment
 */
public class R5Fragment extends BaseFragment {

    private static final String TAG = R5Fragment.class.getSimpleName();

    // 提示信息列表
    private List<Hint> mHints = new ArrayList<>();

    // 提示信息列表适配器
    private HintAdapter mHintAdapter;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagCountChangedMessage message) {
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
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    if (MyVars.cache.insert(CommonUtils.bytesToHex(message.epc))) {
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
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r3;
    }
}
