package com.casc.rfidscanner.fragment;

import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
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
import com.casc.rfidscanner.bean.EPCType;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.param.MsgLog;
import com.casc.rfidscanner.helper.net.param.MsgRegister;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.ProductSelectedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashSet;
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

    // 要注册的产品名称以及要注册的桶EPC
    private String mProductName, mBucketToRegister;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @BindView(R.id.icl_r0_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.ns_r0_registered_count) NumberSwitcher mRegisteredCountNs;
    @BindView(R.id.btn_r0_register) Button mRegisterBtn;
    @BindView(R.id.qrv_r0_body_code_reader) QRCodeReaderView mBodyCodeReaderQrv;
    @BindView(R.id.rl_r0_hint_root) RelativeLayout mHintRootRl;
    @BindView(R.id.iv_r0_tag_status) ImageView mTagStatusIv;
    @BindView(R.id.tv_r0_tag_status) TextView mTagStatusTv;
    @BindView(R.id.tv_r0_hint_content) TextView mHintContentTv;

    @OnClick(R.id.tv_r0_title) void onTitleTextViewClicked() {
        ProductSelectActivity.actionStart(mContext);
    }

    @OnClick(R.id.btn_r0_register) void onRegisterButtonClicked() {
        // 修改注册标志位，禁用界面按钮
        mIsRegistering = true;
        mRegisterBtn.setEnabled(false);
        mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
        mHintContentTv.setText("");
        // EPC组装，并生成Product实例
        byte[] epc = new byte[MyParams.EPC_BUCKET_LENGTH];
        System.arraycopy(CommonUtils.generateEPCHeader(), 0, epc, 0, MyParams.EPC_HEADER_LENGTH);
        epc[MyParams.EPC_TYPE_INDEX] = EPCType.BUCKET.getCode();
        epc[MyParams.EPC_TYPE_INDEX + 1] =
                (byte) MyVars.config.getProductByName(mProductName).getInt();
        String bodyCode = mBodyCodeIcl.getCode().substring(3);
        for (int i = 0; i < bodyCode.length(); i++) {
            epc[MyParams.EPC_TYPE_INDEX + 3 + i] = (byte) bodyCode.charAt(i);
        }
        mBucketToRegister = CommonUtils.bytesToHex(epc);
        // 开始写入任务
        MyVars.executor.execute(new WriteEPCTask());
    }

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
                case NONE: // 检测到未注册桶标签，允许注册
                case BUCKET: // 检测到注册桶标签，允许注册
                    mReadNoneCount = 0;
                    if (mEPCs.contains(epcStr) && mBucketToRegister == null) {
                        mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                        mTagStatusTv.setText("检测到已注册标签");
                    } else {
                        mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
                        if (Arrays.equals(message.epc, mScannedEPC)) { // 判定扫到的EPC是否为前一次扫到的
                            mReadCount += 1;
                            mQualifiedCount += message.rssi >=
                                    SpHelper.getInt(MyParams.S_RSSI_THRESHOLD) ? 1 : 0;
                        } else {
                            mReadCount = 0;
                            mQualifiedCount = 0;
                            mScannedEPC = message.epc;
                            mIsUnregisteredEPCRead = false;
                        }
                        if (mReadCount >= CAN_REGISTER_READ_COUNT) {
                            if (mQualifiedCount < SpHelper.getInt(MyParams.S_MIN_REACH_TIMES)) {
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
                        ConfigActivity.actionStart(mContext, epcStr);
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
        mBodyCodeIcl.setHeader(MyVars.config.getHeader());
        mRegisteredCountNs.setNumber(0);
        mBodyCodeReaderQrv.setOnQRCodeReadListener(this);
        mBodyCodeReaderQrv.setQRDecodingEnabled(true);
        mBodyCodeReaderQrv.setAutofocusInterval(500L);
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

    private void writeTaskSuccess() {
        mEPCs.add(mBucketToRegister);
        Message.obtain(mHandler, MSG_SUCCESS).sendToTarget();
    }

    private void updateTaskSuccess() {
        //mEPCs.add(mBucketToRegister.getEPCStr());
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
        return !mIsRegistering && mIsAllConnectionsReady
                && mIsUnregisteredEPCRead  && mIsBodyCodeWritten;
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
//                    int bodyCode = Integer.valueOf(outer.mBodyCodeIcl.getInt().substring(3));
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

        private class R0Reply {
            @SerializedName("creation_time")
            long creationTime;
        }

        @Override
        public void run() {
            byte[] data, tid, epc = CommonUtils.hexToBytes(mBucketToRegister);

            // 这里出现过一个Bug，当桶EPC从16字节变为12字节时，PC的写入及检测流程始终失败，原因在于MASK的设置因为
            // 桶EPC的长度缩减而变失效了
            if (mScannedEPC.length > MyParams.EPC_BUCKET_LENGTH) {
                mScannedEPC = Arrays.copyOf(mScannedEPC, MyParams.EPC_BUCKET_LENGTH);
            }
            try {
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
                    MyVars.cache.storeLogMessage(MsgLog.warn("读取TID失败:" + mBodyCodeIcl.getCode()));
                    return;
                } else {
                    writeHint("读取TID\n成功");
                    tid = InsHelper.getReadContent(data);
                }

                // 通过向平台发送TID与桶身码的参数，根据返回结果判定是否重复注册
//                String bodyCode = "";
//                if (CommonUtils.validEPC(mScannedEPC) == EPCType.BUCKET) {
//                    bodyCode = CommonUtils.getBodyCode(mScannedEPC);
//                }
                Reply queryReply = NetHelper.getInstance().getRegisterInfo(
                        CommonUtils.bytesToHex(tid), mBodyCodeIcl.getCode()).execute().body();
                if (queryReply == null) {
                    writeHint("注册失败,平台连接失败");
                    writeTaskFailed(true);
                    return;
                } else if (queryReply.getCode() == 211) {
                    writeHint("桶身码已使用,请更换后重试");
                    writeTaskFailed(false);
                    MyVars.cache.storeLogMessage(MsgLog.warn("重复注册:" + mBodyCodeIcl.getCode()));
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
                    MyVars.cache.storeLogMessage(MsgLog.warn("写入PC失败:" + mBodyCodeIcl.getCode()));
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
                    MyVars.cache.storeLogMessage(MsgLog.warn("PC校验失败:" + mBodyCodeIcl.getCode()));
                    return;
                } else {
                    writeHint("PC校验成功");
                }

                // 写入EPC
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        MyParams.EPC_START_INDEX, epc));
                Log.i(TAG, "EPC Write: " + CommonUtils.bytesToHex(data));
                if (data == null) {
                    writeHint("写入EPC\n失败");
                    writeTaskFailed(true);
                    MyVars.cache.storeLogMessage(MsgLog.warn("写入EPC失败:" + mBodyCodeIcl.getCode()));
                    return;
                } else {
                    writeHint("写入EPC\n成功");
                }

                // 设置新Mask
                MyVars.getReader().setMask(epc, 2);

                // 校验EPC
                data = MyVars.getReader().sendCommandSync(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        MyParams.EPC_START_INDEX,
                        MyParams.EPC_BUCKET_LENGTH / 2));
                Log.i(TAG, "EPC Check: " + CommonUtils.bytesToHex(data));
                if (data == null || !Arrays.equals(InsHelper.getReadContent(data), epc)) {
                    writeHint("EPC校验\n失败");
                    writeTaskFailed(true);
                    MyVars.cache.storeLogMessage(MsgLog.warn("EPC校验失败:" + mBodyCodeIcl.getCode()));
                    return;
                } else {
                    writeHint("EPC校验\n成功");
                    //mBucketToRegister.setTID(InsHelper.getReadContent(data));
                }

                // 尝试上报平台
                MsgRegister msg = new MsgRegister(mProductName);
                msg.addBucket(CommonUtils.bytesToHex(epc), CommonUtils.bytesToHex(tid), mBodyCodeIcl.getCode());
                Response<Reply> registerResponse = NetHelper.getInstance().uploadRegisterMsg(msg).execute();
                Reply registerReply = registerResponse.body();
                if (registerReply == null) {
                    writeHint("平台内部错误" + registerResponse.code() + ",请联系运维人员");
                    writeTaskFailed(false);
                    return;
                } else if (registerReply.getCode() != 200) {
                    switch (registerReply.getCode()) {
                        case 210:
                            writeHint("该桶已注册于" + CommonUtils.convertDateTime(
                                    new Gson().fromJson(registerReply.getContent(), R0Reply.class).creationTime * 1000));
                            if (mFailed.contains(mBucketToRegister)) {
                                mFailed.remove(mBucketToRegister);
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
                            MyVars.cache.storeLogMessage(
                                    MsgLog.info("桶身码更新成功:"
                                            + CommonUtils.getBodyCode(mScannedEPC) + " -> "
                                            + CommonUtils.getBodyCode(epc)));
                            return;
                        case 216:
                            writeHint("该桶身码已\n报废");
                            writeTaskFailed(false);
                            return;
                        case 217:
                            writeHint("桶产品信息\n修改成功");
                            updateTaskSuccess();
                            MyVars.cache.storeLogMessage(
                                    MsgLog.info(mBodyCodeIcl.getCode() + "产品信息修改成功:"
                                            + CommonUtils.getProduct(mScannedEPC).getStr() + " -> "
                                            + CommonUtils.getProduct(epc).getStr()));
                            return;
                        case 219:
                            writeHint("该桶已报废");
                            writeTaskFailed(false);
                            return;
                        default:
                            Log.i(TAG, String.valueOf(registerReply.getCode()));
                            writeHint(registerReply.getMessage() + ",请联系运维人员");
                            writeTaskFailed(false);
                            return;
                    }
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
                writeTaskFailed(true);
                mFailed.add(mBucketToRegister);
                return;
            }
            writeHint(mBodyCodeIcl.getCode() + "\n注册成功");
            writeTaskSuccess();
        }
    }
}
