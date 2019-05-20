package com.casc.rfidscanner.fragment;

import android.support.v7.widget.RecyclerView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.NetAdapter;
import com.casc.rfidscanner.helper.net.param.MsgLine;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.TagUploadedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagUploadedMessage msg) {
        mUploadedCountNs.increaseNumber();
        mStoredCountNs.decreaseNumber();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage msg) {
        if (msg.isRead) {
            String epcStr = CommonUtils.bytesToHex(msg.epc);
            switch (CommonUtils.validEPC(msg.epc)) {
                case BUCKET:
                    if (MyVars.cache.insert(epcStr)) {
                        mScannedCountNs.increaseNumber();
                        playSound();
                        final MsgLine msgLine = new MsgLine().addBucket(epcStr);
                        NetHelper.getInstance().uploadProductMsg(msgLine).enqueue(new NetAdapter() {
                            @Override
                            public void onSuccess(Reply reply) {
                                mUploadedCountNs.increaseNumber();
                            }

                            @Override
                            public void onFail(String msg) {
                                mStoredCountNs.increaseNumber();
                                MyVars.cache.storeLineMessage(msgLine);
                            }
                        });
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        ConfigActivity.actionStart(mContext, epcStr);
                    }
                    break;
            }
        } else {
            mAdminCardScannedCount = 0;
        }
    }

    @Override
    protected void initFragment() {
        mScannedCountNs.setNumber(0);
        mUploadedCountNs.setNumber(0);
        mStoredCountNs.setNumber((int) MyVars.cache.getStoredCount());
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r4;
    }
}
