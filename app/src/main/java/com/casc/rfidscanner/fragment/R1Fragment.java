package com.casc.rfidscanner.fragment;

import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.bean.EPCType;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.param.MsgLog;
import com.casc.rfidscanner.helper.net.param.MsgScrap;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Arrays;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Response;

/**
 * 桶报废Fragment
 */
public class R1Fragment extends BaseFragment implements QRCodeReaderView.OnQRCodeReadListener {

    private static final String TAG = R1Fragment.class.getSimpleName();
    private static final int CAN_SCRAP_READ_COUNT = 50;
    private static final int MAX_READ_NONE_COUNT = 20;
    // Constant for InnerHandler message.
    private static final int MSG_SUCCESS = 0;
    private static final int MSG_FAILED = 1;
    private static final int MSG_RESET_READ_STATUS = 2;
    private static final int MSG_UPDATE_HINT = 3;

    // 报废状态的标志符
    private boolean mIsScraping;

    // 当前读取的EPC
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mReadNoneCount;

    // 报废所必需的相关元素标志位
    private boolean mIsBucketEPCRead, mIsReaderReady, mIsNetworkReady, mIsBodyCodeWritten;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @BindView(R.id.icl_r1_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.spn_scrap_reason) BetterSpinner mScrapReasonSpn;
    @BindView(R.id.btn_r1_scrap) Button mScrapBtn;
    @BindView(R.id.qrv_r1_body_code_reader) QRCodeReaderView mBodyCodeReaderQrv;
    @BindView(R.id.iv_r1_tag_status) ImageView mTagStatusIv;
    @BindView(R.id.tv_r1_tag_status) TextView mTagStatusTv;
    @BindView(R.id.tv_r1_hint_content) TextView mHintContentTv;

    @OnClick(R.id.btn_r1_scrap) void onButtonClicked() {
        new MaterialDialog.Builder(mContext)
                .content("桶身码：" + mBodyCodeIcl.getCode() + "\n" + "报废原因：" + mScrapReasonSpn.getText())
                .positiveText("确认报废")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消报废")
                .negativeColorRes(R.color.gray)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        mIsScraping = true;
                        mScrapBtn.setEnabled(false);
                        mHintContentTv.setText("");
                        MyVars.executor.execute(new ScrapTask());
                    }
                })
                .show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        super.onMessageEvent(message);
        if (message.networkStatus && message.platformStatus) {
            mIsNetworkReady = true;
            mScrapBtn.setEnabled(canScrap());
        } else {
            mIsNetworkReady = false;
            mScrapBtn.setEnabled(false);
        }
        mIsReaderReady = message.readerStatus;
        if (!message.readerStatus) {
            mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        updateConfigViews();
        mBodyCodeIcl.setHeader(MyVars.config.getHeader());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case BUCKET:
                    mReadNoneCount = 0;
                    mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
                    if (Arrays.equals(message.epc, mScannedEPC)) {
                        mReadCount += 1;
                    } else {
                        mReadCount = 0;
                        mScannedEPC = message.epc;
                        mIsBucketEPCRead = false;
                    }
                    if (mReadCount >= CAN_SCRAP_READ_COUNT) {
                        mIsBucketEPCRead = true;
                        mBodyCodeIcl.setCode(CommonUtils.getBodyCode(mScannedEPC)
                                .substring(MyParams.BODY_CODE_HEADER));
                        mTagStatusTv.setText("标签信号正常");
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
            if (++mReadNoneCount >= MAX_READ_NONE_COUNT) {
                if (mIsBucketEPCRead) {
                    mBodyCodeIcl.clear();
                }
                Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                if (!mIsScraping) {
                    mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                    mTagStatusTv.setText("未检测到标签");
                }
            }
        }
    }

    @Override
    protected void initFragment() {
        mBodyCodeIcl.setHeader(MyVars.config.getHeader());
        mBodyCodeReaderQrv.setOnQRCodeReadListener(this);
        mBodyCodeReaderQrv.setQRDecodingEnabled(true);
        mBodyCodeReaderQrv.setAutofocusInterval(250L);
        mBodyCodeReaderQrv.setTorchEnabled(true);
        mBodyCodeReaderQrv.setBackCamera();

        updateConfigViews();
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r1;
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        if (mIsBucketEPCRead) {
            showToast("通过二维码报废时请将天线门中的桶移开");
        } else {
            mVibrator.vibrate(50);
            String[] result = text.split("=");
            if (result.length == 2 && result[1].length() == MyParams.BODY_CODE_LENGTH
                    && result[1].startsWith(MyVars.config.getHeader())) {
                mBodyCodeIcl.setCode(result[1].substring(MyVars.config.getHeader().length()));
                mIsBodyCodeWritten = true;
                mScrapBtn.setEnabled(canScrap());
            }
        }

    }

    private void writeTaskSuccess() {
        Message.obtain(mHandler, MSG_SUCCESS).sendToTarget();
    }

    private void writeTaskFailed() {
        Message.obtain(mHandler, MSG_FAILED).sendToTarget();
    }

    private void writeHint(String content) {
        Message.obtain(mHandler, MSG_UPDATE_HINT, content).sendToTarget();
    }

    private boolean canScrap() {
        return ((mIsReaderReady && mIsBucketEPCRead) || mIsBodyCodeWritten) && mIsNetworkReady;
    }

    private void updateConfigViews() {
        mScrapReasonSpn.setAdapter(new ArrayAdapter<>(mContext, R.layout.item_common,
                MyVars.config.getDisables()));

        String curScrapReason = mScrapReasonSpn.getText().toString();
        if (TextUtils.isEmpty(curScrapReason)
                || MyVars.config.getDisableByWord(curScrapReason) == null) {
            if (!MyVars.config.getDisables().isEmpty()) {
                mScrapReasonSpn.setText(MyVars.config.getDisables().get(0).getStr());
            } else {
                mScrapReasonSpn.setText("");
            }
        }
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R1Fragment> mOuter;

        InnerHandler(R1Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R1Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_SUCCESS:
                    outer.mBodyCodeIcl.clear();
                    outer.mIsBodyCodeWritten = false;
                    outer.mIsScraping = false;
                    outer.playSound();
                    Message.obtain(outer.mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                    SpeechSynthesizer.getInstance().speak("报废成功");
                    break;
                case MSG_FAILED:
                    outer.mBodyCodeIcl.clear();
                    outer.mIsBodyCodeWritten = false;
                    outer.mIsScraping = false;
                    Message.obtain(outer.mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                    SpeechSynthesizer.getInstance().speak("报废失败");
                    break;
                case MSG_RESET_READ_STATUS:
                    outer.mReadCount = 0;
                    outer.mScannedEPC = null;
                    outer.mIsBucketEPCRead = false;
                    outer.mScrapBtn.setEnabled(outer.canScrap());
                    break;
                case MSG_UPDATE_HINT:
                    outer.mHintContentTv.setText((String) msg.obj);
                    break;
            }
        }
    }

    private class ScrapTask implements Runnable {

        @Override
        public void run() {
            byte[] data, tid;
            try {
                MsgScrap msg = new MsgScrap();
                if (mIsBucketEPCRead) {
                    if (mScannedEPC.length > MyParams.EPC_BUCKET_LENGTH) {
                        mScannedEPC = Arrays.copyOf(mScannedEPC, MyParams.EPC_BUCKET_LENGTH);
                    }

                    // 设置Mask
                    MyVars.getReader().setMask(mScannedEPC, 2);

                    // 尝试读取TID
                    data = MyVars.getReader().sendCommandSync(InsHelper.getReadMemBank(
                            CommonUtils.hexToBytes("00000000"),
                            InsHelper.MemBankType.TID,
                            MyParams.TID_START_INDEX,
                            MyParams.TID_READ_LENGTH));
                    Log.i(TAG, "TID Read: " + CommonUtils.bytesToHex(data));
                    if (data == null) {
                        writeHint("读取TID\n失败");
                        writeTaskFailed();
                        MyVars.cache.storeLogMessage(MsgLog.warn("读取TID失败:" + mBodyCodeIcl.getCode()));
                        return;
                    } else {
                        writeHint("读取TID\n成功");
                        tid = InsHelper.getReadContent(data);
                    }

                    // 写入EPC
                    byte[] modifiedEPC = new byte[]{EPCType.BUCKET_SCRAPED.getCode(),
                            mScannedEPC[MyParams.EPC_TYPE_INDEX + 1]};
                    data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                            CommonUtils.hexToBytes("00000000"),
                            InsHelper.MemBankType.EPC,
                            MyParams.EPC_START_INDEX + MyParams.EPC_TYPE_INDEX / 2,
                            modifiedEPC));
                    Log.i(TAG, "EPC Write: " + CommonUtils.bytesToHex(data));
                    if (data == null) {
                        writeHint("写入EPC\n失败");
                        writeTaskFailed();
                        MyVars.cache.storeLogMessage(MsgLog.warn("写入EPC失败:" + mBodyCodeIcl.getCode()));
                        return;
                    } else {
                        writeHint("写入EPC\n成功");
                    }

                    msg.addBucket(CommonUtils.bytesToHex(mScannedEPC), CommonUtils.bytesToHex(tid),
                            MyVars.config.getDisableByWord(mScrapReasonSpn.getText().toString()).getInt());
                } else {
                    msg.addBucket(mBodyCodeIcl.getCode(),
                            MyVars.config.getDisableByWord(mScrapReasonSpn.getText().toString()).getInt());
                }

                // 尝试上报平台
                Response<Reply> responseR1 = NetHelper.getInstance().uploadScrapMsg(msg).execute();
                Reply replyR1 = responseR1.body();
                if (replyR1 == null) {
                    writeHint("平台内部错误" + responseR1.code() + ",请联系运维人员");
                    writeTaskFailed();
                    return;
                } else if (replyR1.getCode() != 200) {
                    writeHint("平台返回\n异常:" + replyR1.getCode());
                    writeTaskFailed();
                    return;
                } else {
                    writeHint("上报平台\n成功");
                }
            } catch (Exception e) {
                if (e instanceof ConnectException) {
                    writeHint("服务器不可用,请检查网络");
                }
                else if (e instanceof SocketTimeoutException) {
                    writeHint("网络连接超时,请稍后重试");
                } else {
                    writeHint("网络连接失败(" + e.getMessage() + ")");
                }
                writeTaskFailed();
                return;
            }
            writeHint((mIsBucketEPCRead ? CommonUtils.getBodyCode(mScannedEPC) : mBodyCodeIcl.getCode()) + "报废成功");
            writeTaskSuccess();
        }
    }
}
