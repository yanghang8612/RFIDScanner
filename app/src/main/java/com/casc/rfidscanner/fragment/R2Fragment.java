package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.BillConfirmActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.DealerAndDriverSelectedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    @BindView(R.id.icl_r2_unknown_count) InputCodeLayout mUnknownCountIcl;
    @BindView(R.id.iv_add_count) ImageView mAddCountIv;
    @BindView(R.id.iv_minus_count) ImageView mMinusCountIv;
    @BindView(R.id.ns_r2_scanned_count) NumberSwitcher mScannedCountNs;
    @BindView(R.id.btn_r2_commit) Button mCommitBtn;
    @BindView(R.id.qrv_r2_body_code_reader) QRCodeReaderView mBodyCodeReaderQrv;
    @BindView(R.id.tv_r2_hint_content) TextView mHintContentTv;

    // 当前读取的EPC
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mQualifiedCount, mReadNoneCount;

    private int mUnknownCount;

    private Map<String, Bucket> mEPCBuckets = new HashMap<>();

    private Map<String, Long> mBodyCodes = new HashMap<>();

    // 系统震动辅助类
    private Vibrator mVibrator;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(DealerAndDriverSelectedMessage message) {
        MessageReflux reflux = new MessageReflux(message.dealer, message.driver, 0);
        for (Bucket bucket : mEPCBuckets.values()) {
            reflux.addBucket(bucket.getTime(), bucket.getEpcStr(), bucket.getBodyCode());
        }
        for (String bodyCode : mBodyCodes.keySet()) {
            reflux.addBucket(mBodyCodes.get(bodyCode), "", bodyCode);
        }
        MyVars.cache.storeRefluxBill(reflux);
        showToast("提交成功");
        mEPCBuckets.clear();
        mBodyCodes.clear();
        mUnknownCount = -1;
        mUnknownCountIcl.clear();
        mScannedCountNs.setNumber(0);
        Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    mReadNoneCount = 0;
                    if (Arrays.equals(message.epc, mScannedEPC)) { // 判定扫到的EPC是否为前一次扫到的
                        mReadCount += 1;
                        mQualifiedCount += message.rssi >=
                                ConfigHelper.getInt(MyParams.S_RSSI_THRESHOLD) ? 1 : 0;
                    } else {
                        mReadCount = 0;
                        mQualifiedCount = 0;
                        mScannedEPC = message.epc;
                    }
                    if (mReadCount >= FOUND_READ_COUNT) {
                        String bodyCode = Bucket.getBodyCode(epcStr);
                        if (mBodyCodes.containsKey(bodyCode)) {
                            writeHint(bodyCode + "\n已回收过");
                        } else if (!mEPCBuckets.containsKey(bodyCode) &&
                                mQualifiedCount >= ConfigHelper.getInt(MyParams.S_MIN_REACH_TIMES)) {
                            playSound();
                            Bucket bucket = new Bucket(mScannedEPC);
                            mEPCBuckets.put(bodyCode, bucket);
                            mScannedCountNs.increaseNumber();
                            writeHint(bucket.getBodyCode() + "\n回收成功");
                        }
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                        sendAdminLoginMessage(CommonUtils.bytesToHex(message.epc));
                        ConfigActivity.actionStart(mContext);
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
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

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
            if (mEPCBuckets.containsKey(bodyCode) || mBodyCodes.containsKey(bodyCode)) {
                writeHint(bodyCode + "\n已回收过");
            } else {
                playSound();
                mVibrator.vibrate(50);
                mBodyCodes.put(bodyCode, System.currentTimeMillis());
                mScannedCountNs.increaseNumber();
                writeHint(bodyCode + "\n回收成功");
            }
        }
    }

    @OnClick(R.id.iv_add_count)
    void onAddCountImageViewClicked() {
        String count = mUnknownCountIcl.getCode();
        if (TextUtils.isEmpty(count)) {
            mUnknownCount = 0;
        } else {
            mUnknownCount += 1;
        }
        mUnknownCountIcl.setCode(String.format("%03d", mUnknownCount));
    }

    @OnClick(R.id.iv_minus_count)
    void onMinusCountImageViewClicked() {
        String count = mUnknownCountIcl.getCode();
        if (TextUtils.isEmpty(count)) {
            mUnknownCount = 0;
        } else {
            mUnknownCount -= mUnknownCount == 0 ? 0 : 1;
        }
        mUnknownCountIcl.setCode(String.format("%03d", mUnknownCount));
    }

    @OnClick(R.id.btn_r2_commit)
    void onCommitButtonClicked() {
        if (mUnknownCount == -1) {
            showToast("请先选择双重损坏数量");
        } else {
            BillConfirmActivity.actionStart(mContext);
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
