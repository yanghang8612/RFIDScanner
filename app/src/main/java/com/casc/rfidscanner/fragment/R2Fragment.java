package com.casc.rfidscanner.fragment;

import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.BillConfirmActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.helper.net.param.MsgLog;
import com.casc.rfidscanner.helper.net.param.MsgReflux;
import com.casc.rfidscanner.message.BillConfirmedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;

/**
 * 空桶回流Fragment
 */
public class R2Fragment extends BaseFragment implements QRCodeReaderView.OnQRCodeReadListener {

    private static final String TAG = R2Fragment.class.getSimpleName();
    private static final int FOUND_READ_COUNT = 20;
    private static final int MAX_READ_NONE_COUNT = 10;
    // Constant for InnerHandler message.what
    private static final int MSG_RESET_READ_STATUS = 0;
    private static final int MSG_UPDATE_HINT = 1;

    // 当前读取的EPC
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mQualifiedCount, mReadNoneCount;

    private int mUnknownCount;

    private Map<String, Long> mBuckets = new HashMap<>();

    private Map<String, Long> mBodyCodes = new HashMap<>();

    private Set<String> mErrors = new HashSet<>();

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @BindView(R.id.icl_r2_unknown_count) InputCodeLayout mUnknownCountIcl;
    @BindView(R.id.iv_add_count) ImageView mAddCountIv;
    @BindView(R.id.iv_minus_count) ImageView mMinusCountIv;
    @BindView(R.id.ns_r2_scanned_count) NumberSwitcher mScannedCountNs;
    @BindView(R.id.btn_r2_commit) Button mCommitBtn;
    @BindView(R.id.qrv_r2_body_code_reader) QRCodeReaderView mBodyCodeReaderQrv;
    @BindView(R.id.tv_r2_hint_content) TextView mHintContentTv;

    @OnClick(R.id.iv_add_count) void onAddCountImageViewClicked() {
        String count = mUnknownCountIcl.getCode();
        if (TextUtils.isEmpty(count)) {
            mUnknownCount = 0;
        } else {
            mUnknownCount += 1;
        }
        mUnknownCountIcl.setCode(String.format(Locale.CHINA, "%03d", mUnknownCount));
    }

    @OnClick(R.id.iv_minus_count) void onMinusCountImageViewClicked() {
        String count = mUnknownCountIcl.getCode();
        if (TextUtils.isEmpty(count)) {
            mUnknownCount = 0;
        } else {
            mUnknownCount -= mUnknownCount == 0 ? 0 : 1;
        }
        mUnknownCountIcl.setCode(String.format(Locale.CHINA, "%03d", mUnknownCount));
    }

    @OnClick(R.id.btn_r2_commit) void onCommitButtonClicked() {
        if (mUnknownCount == -1) {
            showToast("请先选择双重损坏数量");
        } else {
            BillConfirmActivity.actionStart(mContext);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillConfirmedMessage message) {
        MsgReflux reflux = new MsgReflux(message.dealer, message.driver, mUnknownCount);
        for (Map.Entry<String, Long> bucket : mBuckets.entrySet()) {
            reflux.addBucket(bucket.getKey(), bucket.getValue(), CommonUtils.getBodyCode(bucket.getKey()));
        }
        for (Map.Entry<String, Long> bodyCode : mBodyCodes.entrySet()) {
            reflux.addBucket("", bodyCode.getValue(), bodyCode.getKey());
        }
        MyVars.cache.storeRefluxBill(reflux);
        showToast("提交成功");
        mBuckets.clear();
        mBodyCodes.clear();
        mUnknownCount = -1;
        mUnknownCountIcl.clear();
        mScannedCountNs.setNumber(0);
        Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    mReadNoneCount = 0;
                    writeHint("检测到\n未注册标签");
                    if (!mErrors.contains(epcStr)) {
                        mErrors.add(epcStr);
                        MyVars.cache.storeLogMessage(MsgLog.warn("检测到未注册标签：" + epcStr));
                    }
                    break;
                case BUCKET:
                    mReadNoneCount = 0;
                    if (Arrays.equals(message.epc, mScannedEPC)) { // 判定扫到的EPC是否为前一次扫到的
                        mReadCount += 1;
                        mQualifiedCount += message.rssi >=
                                SpHelper.getInt(MyParams.S_RSSI_THRESHOLD) ? 1 : 0;
                    } else {
                        mReadCount = 0;
                        mQualifiedCount = 0;
                        mScannedEPC = message.epc;
                    }
                    if (mReadCount >= FOUND_READ_COUNT) {
                        String bodyCode = CommonUtils.getBodyCode(epcStr);
                        if (mBodyCodes.containsKey(bodyCode)) {
                            writeHint(bodyCode + "\n已回收过");
                        } else if (!mBuckets.containsKey(epcStr) &&
                                mQualifiedCount >= SpHelper.getInt(MyParams.S_MIN_REACH_TIMES)) {
                            playSound();
                            mBuckets.put(epcStr, System.currentTimeMillis());
                            mScannedCountNs.increaseNumber();
                            writeHint(bodyCode + "\n回收成功");
                        }
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                        ConfigActivity.actionStart(mContext, epcStr);
                    }
                    break;
            }
        } else {
            mAdminCardScannedCount = 0;
            if (++mReadNoneCount == MAX_READ_NONE_COUNT) {
                writeHint("");
                Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
            }
        }
    }

    @Override
    protected void initFragment() {
        mUnknownCount = -1;
        mScannedCountNs.setNumber(0);
        mBodyCodeReaderQrv.setOnQRCodeReadListener(this);
        mBodyCodeReaderQrv.setQRDecodingEnabled(true);
        mBodyCodeReaderQrv.setAutofocusInterval(250L);
        mBodyCodeReaderQrv.setTorchEnabled(true);
        mBodyCodeReaderQrv.setBackCamera();
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r2;
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        String[] result = text.split("=");
        if (result.length == 2 && result[1].length() == MyParams.BODY_CODE_LENGTH
                && result[1].startsWith(MyVars.config.getHeader())) {
            String bodyCode = result[1];
            if (mBodyCodes.containsKey(bodyCode)) {
                writeHint(bodyCode + "\n已回收过");
                return;
            }
            for (String epcStr : mBuckets.keySet()) {
                if (bodyCode.equals(CommonUtils.getBodyCode(epcStr))) {
                    writeHint(bodyCode + "\n已回收过");
                    return;
                }
            }
            playSound();
            mVibrator.vibrate(50);
            mBodyCodes.put(bodyCode, System.currentTimeMillis());
            mScannedCountNs.increaseNumber();
            writeHint(bodyCode + "\n回收成功");
        }
    }

    private void writeHint(String content) {
        Message.obtain(mHandler, MSG_UPDATE_HINT, content).sendToTarget();
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
                case MSG_RESET_READ_STATUS:
                    outer.mReadCount = 0;
                    outer.mScannedEPC = null;
                    break;
                case MSG_UPDATE_HINT:
                    outer.mHintContentTv.setText((String) msg.obj);
                    break;
            }
        }
    }
}
