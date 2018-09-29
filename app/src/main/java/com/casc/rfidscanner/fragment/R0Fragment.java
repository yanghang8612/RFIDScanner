package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.ErrorRemindActivity;
import com.casc.rfidscanner.activity.ProductSelectActivity;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.ProductSelectedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Response;

/**
 * 桶注册Fragment
 */
public class R0Fragment extends BaseFragment implements QRCodeReaderView.OnQRCodeReadListener {

    private static final String TAG = R0Fragment.class.getSimpleName();
    private static final int CAN_REGISTER_READ_COUNT = 50;
    private static final int MAX_READ_NONE_COUNT = 20;
    // Constant for InnerHandler message.what
    private static final int MSG_SUCCESS = 0;
    private static final int MSG_UPDATED = 1;
    private static final int MSG_FAILED = 2;
    private static final int MSG_RESET_READ_STATUS = 3;
    private static final int MSG_UPDATE_HINT = 4;

    @BindView(R.id.icl_r0_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.ns_r0_registered_count) NumberSwitcher mRegisteredCountNs;
    @BindView(R.id.btn_r0_register) Button mRegisterBtn;
    @BindView(R.id.qrv_r0_body_code_reader) QRCodeReaderView mBodyCodeReaderQrv;
    @BindView(R.id.rl_r0_hint_root) RelativeLayout mHintRootRl;
    @BindView(R.id.iv_r0_tag_status) ImageView mTagStatusIv;
    @BindView(R.id.tv_r0_tag_status) TextView mTagStatusTv;
    @BindView(R.id.tv_r0_hint_content) TextView mHintContentTv;

    // 已注册桶列表
    private Set<String> mEPCs = new HashSet<>();

    // 未收到平台响应的桶列表
    private Set<String> mFailed = new HashSet<>();

    // 注册状态的标志符
    private boolean mIsRegistering;

    // 当前读取的EPC
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mQualifiedCount, mReadNoneCount;

    // 注册所必需的相关元素标志位
    private boolean mIsUnregisteredEPCRead, mIsAllConnectionsReady, mIsBodyCodeWritten;

    // 要注册的产品名称
    private String mProductName;

    // 要注册的桶实例
    private Bucket mBucketToRegister;

    // 系统震动辅助类
    private Vibrator mVibrator;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        super.onMessageEvent(message);
        if (message.readerStatus && message.networkStatus && message.platformStatus) {
            mIsAllConnectionsReady = true;
            mRegisterBtn.setEnabled(canRegister());
        } else {
            mIsAllConnectionsReady = false;
            mRegisterBtn.setEnabled(false);
        }
        if (!message.readerStatus) {
            mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
            if (mIsRegistering) {
                mIsRegistering = false;
                mRegisterBtn.setEnabled(false);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        mBodyCodeIcl.setHeader(MyVars.config.getHeader());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(mScannedEPC)) {
                case BUCKET: // 检测到注册桶标签，也允许注册
                case NONE: // 检测到未注册桶标签，允许注册
                    mReadNoneCount = 0;
                    if (mEPCs.contains(epcStr) && mBucketToRegister == null) {
                        mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                        mTagStatusTv.setText("检测到已注册标签");
                    } else {
                        mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
                        if (Arrays.equals(message.epc, mScannedEPC)) { // 判定扫到的EPC是否为前一次扫到的
                            mReadCount += 1;
                            mQualifiedCount += message.rssi >=
                                    ConfigHelper.getInt(MyParams.S_RSSI_THRESHOLD) ? 1 : 0;
                        } else {
                            mReadCount = 0;
                            mQualifiedCount = 0;
                            mScannedEPC = message.epc;
                            mIsUnregisteredEPCRead = false;
                        }
                        if (mReadCount >= CAN_REGISTER_READ_COUNT) {
                            if (mQualifiedCount < ConfigHelper.getInt(MyParams.S_MIN_REACH_TIMES)) {
                                mTagStatusTv.setText("标签信号弱");
//                                mTagStatusTv.setTextColor(mContext.getColor(R.color.indian_red));
                            } else {
                                mIsUnregisteredEPCRead = true;
                                mRegisterBtn.setEnabled(canRegister());
                                mTagStatusTv.setText("标签信号正常");
//                                mTagStatusTv.setTextColor(mContext.getColor(R.color.green));
                            }
                        }
                    }
                    break;
                case BUCKET_SCRAPED:
                    mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                    mTagStatusTv.setText("检测到已报废标签");
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) { // 启动配置界面，并暂停EPC读取
                        Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                        sendAdminLoginMessage(CommonUtils.bytesToHex(message.epc));
                        ConfigActivity.actionStart(mContext);
                    }
                    break;
                default:
                    Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
            }
        } else {
            mAdminCardScannedCount = 0;
            if (++mReadNoneCount >= MAX_READ_NONE_COUNT) {
                mHintContentTv.setText(mProductName);
                mHintRootRl.setBackground(mContext.getDrawable(R.drawable.bg_r0_normal));
                mRegisterBtn.setEnabled(false);
                Message.obtain(mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                if (!mIsRegistering) {
                    mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                    mTagStatusTv.setText("未检测到标签");
//                    mTagStatusTv.setTextColor(mContext.getColor(R.color.black));
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ProductSelectedMessage message) {
        mProductName = message.product;
        mRegisteredCountNs.setNumber(0);
        writeHint(mProductName);
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        mBodyCodeIcl.setHeader(MyVars.config.getHeader());
        mRegisteredCountNs.setNumber(0);
        mBodyCodeReaderQrv.setOnQRCodeReadListener(this);
        mBodyCodeReaderQrv.setQRDecodingEnabled(true);
        mBodyCodeReaderQrv.setAutofocusInterval(100L);
        mBodyCodeReaderQrv.setTorchEnabled(true);
        mBodyCodeReaderQrv.setBackCamera();

        ProductSelectActivity.actionStart(mContext);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r0;
    }

    @Override
    public void onQRCodeRead(String text, PointF[] points) {
        mVibrator.vibrate(50);
        String[] result = text.split("=");
        if (result.length == 2 && result[1].length() == MyParams.BODY_CODE_LENGTH
                && result[1].startsWith(MyVars.config.getHeader())) {
            mBodyCodeIcl.setCode(result[1].substring(MyVars.config.getHeader().length()));
            mIsBodyCodeWritten = true;
            mRegisterBtn.setEnabled(canRegister());
//            if (mRegisterBtn.isEnabled()) {
//                onRegisterButtonClicked();
//            }
        }
    }

    @OnClick(R.id.tv_r0_title)
    void onTitleTextViewClicked() {
        ProductSelectActivity.actionStart(mContext);
    }

    @OnClick(R.id.btn_r0_register)
    void onRegisterButtonClicked() {
        // 修改注册标志位，禁用界面按钮
        mIsRegistering = true;
        mRegisterBtn.setEnabled(false);
        mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
        mHintContentTv.setText("");
        // EPC组装，并生成Product实例
        byte[] epc = new byte[MyParams.EPC_BUCKET_LENGTH];
        System.arraycopy(CommonUtils.generateEPCHeader(), 0, epc, 0, MyParams.EPC_HEADER_LENGTH);
        epc[MyParams.EPC_TYPE_INDEX] = MyParams.EPCType.BUCKET.getCode();
        epc[MyParams.EPC_TYPE_INDEX + 1] =
                (byte) MyVars.config.getProductInfoByName(mProductName).getCode();
        String bodyCode = mBodyCodeIcl.getCode().substring(3);
        for (int i = 0; i < bodyCode.length(); i++) {
            epc[MyParams.EPC_TYPE_INDEX + 3 + i] = (byte) bodyCode.charAt(i);
        }
        mBucketToRegister = new Bucket(epc);
        // 开始写入任务
        MyVars.executor.execute(new WriteEPCTask());
    }

    private void writeTaskSuccess() {
        mEPCs.add(mBucketToRegister.getEpcStr());
        Message.obtain(mHandler, MSG_SUCCESS).sendToTarget();
    }

    private void updateTaskSuccess() {
        //mEPCs.add(mBucketToRegister.getEpcStr());
        Message.obtain(mHandler, MSG_UPDATED).sendToTarget();
    }

    private void writeTaskFailed(boolean isShowRemind) {
        if (isShowRemind) {
            ErrorRemindActivity.actionStart(mContext);
        }
        Message.obtain(mHandler, MSG_FAILED).sendToTarget();
    }

    private void writeHint(String content) {
        Message.obtain(mHandler, MSG_UPDATE_HINT, content).sendToTarget();
    }

    private boolean canRegister() {
        return MyVars.getReader().isConnected() & !mIsRegistering &&
                mIsUnregisteredEPCRead && mIsAllConnectionsReady && mIsBodyCodeWritten;
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R0Fragment> mOuter;

        InnerHandler(R0Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R0Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_SUCCESS:
                    outer.mRegisteredCountNs.increaseNumber();
                case MSG_UPDATED:
                    outer.mHintRootRl.setBackground(outer.mContext.getDrawable(R.drawable.bg_r0_success));
                    outer.mBodyCodeIcl.clear();
                    outer.mIsBodyCodeWritten = false;
//                    int bodyCode = Integer.valueOf(outer.mBodyCodeIcl.getCode().substring(3));
//                    outer.mBodyCodeIcl.setCode(String.format("%05d", ++bodyCode));
                    outer.mIsRegistering = false;
                    outer.mRegisterBtn.setEnabled(false);
                    outer.playSound();
                    Message.obtain(outer.mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                    //SpeechSynthesizer.getInstance().speak("注册成功");
                    break;
                case MSG_FAILED:
                    outer.mHintRootRl.setBackground(outer.mContext.getDrawable(R.drawable.bg_r0_failed));
                    outer.mBodyCodeIcl.clear();
                    outer.mIsBodyCodeWritten = false;
                    outer.mIsRegistering = false;
                    outer.mRegisterBtn.setEnabled(outer.canRegister());
                    Message.obtain(outer.mHandler, MSG_RESET_READ_STATUS).sendToTarget();
                    SpeechSynthesizer.getInstance().speak("注册失败");
                    break;
                case MSG_RESET_READ_STATUS:
                    outer.mReadCount = 0;
                    outer.mScannedEPC = null;
                    outer.mBucketToRegister = null;
                    outer.mIsUnregisteredEPCRead = false;
                    break;
                case MSG_UPDATE_HINT:
                    outer.mHintContentTv.setText((String) msg.obj);
                    break;
            }
        }
    }

    private class WriteEPCTask implements Runnable {

        @Override
        public void run() {
            byte[] data;

            // 这里出现过一个Bug，当桶EPC从16字节变为12字节时，PC的写入及检测流程始终失败，原因在于MASK的设置因为
            // 桶EPC的长度缩减而变失效了
            if (mScannedEPC.length > MyParams.EPC_BUCKET_LENGTH) {
                mScannedEPC = Arrays.copyOf(mScannedEPC, MyParams.EPC_BUCKET_LENGTH);
            }
            try {
                Log.i(TAG, "EPC Target:" + CommonUtils.bytesToHex(mBucketToRegister.getEpc()));
                Log.i(TAG, "EPC Source:" + CommonUtils.bytesToHex(mScannedEPC));

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
                    writeTaskFailed(true);
                    return;
                } else {
                    writeHint("读取TID\n成功");
                    mBucketToRegister.setTid(InsHelper.getReadContent(data));
                }

                // 通过向平台发送TID与桶身码的参数，根据返回结果判定是否重复注册
                String bodyCode = "";
                if (CommonUtils.validEPC(mScannedEPC) == MyParams.EPCType.BUCKET) {
                    bodyCode = new Bucket(mScannedEPC).getBodyCode();
                }
                MessageQuery messageQuery = new MessageQuery(
                        CommonUtils.bytesToHex(mBucketToRegister.getTid()), bodyCode);
                Response<Reply> queryResponse = NetHelper.getInstance().checkBodyCodeAndTID(messageQuery).execute();
                Reply replyQuery = queryResponse.body();
                Log.i(TAG, "" + replyQuery.getCode());
                if (!queryResponse.isSuccessful() || replyQuery == null) {
                    writeHint("注册失败,平台连接失败");
                    writeTaskFailed(true);
                    return;
                } else if (replyQuery.getCode() == 211) {
                    writeHint("桶身码已使用,请更换后重试");
                    writeTaskFailed(false);
                    return;
                }

                // 写入PC
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        MyParams.PC_START_INDEX,
                        CommonUtils.generatePC(MyParams.EPC_BUCKET_LENGTH)));
                // 这里不能检查epc的长度，因为修改PC后返回的EPC是修改之前的长度，而实际长度可能会发生变化
                Log.i(TAG, "PC Write: " + CommonUtils.bytesToHex(data));
                if (data == null) {
                    writeHint("写入PC失败");
                    writeTaskFailed(true);
                    return;
                } else {
                    writeHint("写入PC成功");
                }

                // 校验PC
                data = MyVars.getReader().sendCommandSync(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        MyParams.PC_START_INDEX,
                        1));
                Log.i(TAG, "PC Check: " + CommonUtils.bytesToHex(data));
                if (data == null || (InsHelper.getReadContent(data)[0] & 0xFF) >> 3
                        != MyParams.EPC_BUCKET_LENGTH / 2) {
                    writeHint( "PC校验失败");
                    writeTaskFailed(true);
                    return;
                } else {
                    writeHint("校验PC成功");
                }

                // 写入EPC
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        MyParams.EPC_START_INDEX,
                        mBucketToRegister.getEpc()));
                Log.i(TAG, "EPC Write: " + CommonUtils.bytesToHex(data));
                if (data == null) {
                    writeHint("写入EPC\n失败");
                    writeTaskFailed(true);
                    return;
                } else {
                    writeHint("写入EPC\n成功");
                }

                // 设置新Mask
                MyVars.getReader().setMask(mBucketToRegister.getEpc(), 2);

                // 校验EPC
                data = MyVars.getReader().sendCommandSync(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        MyParams.EPC_START_INDEX,
                        MyParams.EPC_BUCKET_LENGTH / 2));
                Log.i(TAG, "EPC Check: " + CommonUtils.bytesToHex(data));
                if (data == null || !Arrays.equals(InsHelper.getReadContent(data), mBucketToRegister.getEpc())) {
                    writeHint("EPC校验\n失败");
                    writeTaskFailed(true);
                    return;
                } else {
                    writeHint("校验EPC\n成功");
                    //mBucketToRegister.setTid(InsHelper.getReadContent(data));
                }

                // 尝试上报平台
                MessageRegister message = new MessageRegister(mProductName);
                message.addBucket(
                        CommonUtils.bytesToHex(mBucketToRegister.getTid()),
                        CommonUtils.bytesToHex(mBucketToRegister.getEpc()),
                        mBucketToRegister.getBodyCode());
                Response<Reply> responseR0 = NetHelper.getInstance().uploadRegisterMessage(message).execute();
                Reply replyR0 = responseR0.body();
                if (!responseR0.isSuccessful() || replyR0 == null) {
                    writeHint("平台内部错误,请联系运维人员");
                    writeTaskFailed(false);
                    return;
                } else if (replyR0.getCode() != 200) {
                    switch (replyR0.getCode()) {
                        case 210:
                            writeHint("该桶已于" +
                                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
                                            .format(new Date(Long.valueOf(replyR0.getContent().toString()) * 1000)) + "注册");
                            if (mFailed.contains(mBucketToRegister.getEpcStr())) {
                                mFailed.remove(mBucketToRegister.getEpcStr());
                                writeTaskSuccess();
                            } else {
                                writeTaskFailed(false);
                            }
                            return;
                        case 211:
                            writeHint("桶身码已使用,请更换后重试");
                            writeTaskFailed(false);
                            return;
                        case 215:
                            writeHint("桶身码更新\n成功");
                            updateTaskSuccess();
                            return;
                        case 216:
                            writeHint("该桶身码已\n报废");
                            writeTaskFailed(false);
                            return;
                        case 217:
                            writeHint("桶产品信息\n修改成功");
                            updateTaskSuccess();
                            return;
                        case 219:
                            writeHint("该桶已报废");
                            writeTaskFailed(false);
                            return;
                        default:
                            Log.i(TAG, String.valueOf(replyR0.getCode()));
                            writeHint(replyR0.getMessage() + ",请联系运维");
                            writeTaskFailed(false);
                            return;
                    }
                } else {
                    writeHint("上报平台\n成功");
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeHint("网络通信\n失败");
                writeTaskFailed(true);
                mFailed.add(mBucketToRegister.getEpcStr());
                return;
            }
            writeHint(mBucketToRegister.getBodyCode() + "\n注册成功");
            writeTaskSuccess();
        }
    }
}
