package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.graphics.PointF;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.RegisteredCountAdapter;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.dlazaro66.qrcodereaderview.QRCodeReaderView;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Response;

/**
 * 桶注册Fragment
 */
public class R0Fragment extends BaseFragment implements InsHandler, QRCodeReaderView.OnQRCodeReadListener {

    private static final String TAG = R0Fragment.class.getSimpleName();
    private static final int READ_MAX_TRY_COUNT = 5;
    private static final int WRITE_PC_MAX_TRY_COUNT = 5;
    private static final int WRITE_EPC_MAX_TRY_COUNT = 5;
    private static final int CAN_REGISTER_READ_COUNT = 5;
    private static final int MAX_READ_NONE_COUNT = 3;
    // Constant for InnerHandler message.what
    private static final int MSG_SUCCESS = 0;
    private static final int MSG_FAILED = 1;
    private static final int MSG_UPDATE_HINT = 2;

    @BindView(R.id.icl_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.btn_r0_register) Button mRegisterBtn;
    @BindView(R.id.qrv_body_code_reader) QRCodeReaderView mBodyCodeReaderQrv;
    @BindView(R.id.spn_product_name) BetterSpinner mProductNameSpn;
    @BindView(R.id.rv_registered_count_list) RecyclerView mRegisteredCountListRv;
    @BindView(R.id.iv_tag_status) ImageView mTagStatusIv;
    @BindView(R.id.tv_r0_hint) TextView mHintContentTv;

    private RegisteredCountAdapter mAdapter;

    // 已注册桶列表
    private List<Bucket> mBuckets = new ArrayList<>();

    // 注册状态的标志符
    private boolean mIsRegistering;

    // 当前读取的EPC和数据存储区数据
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mReadNoneCount;

    // 注册所必需的相关元素标志位
    private boolean mIsUnregisteredEPCRead, mIsAllConnectionsReady, mIsBodyCodeWritten;

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
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        updateConfigViews();
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        updateConfigViews();
        MyVars.registeredBuckets = mBuckets;
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        mAdapter = new RegisteredCountAdapter();
        mRegisteredCountListRv.setLayoutManager(new GridLayoutManager(this.getContext(), 2));
        mRegisteredCountListRv.setAdapter(mAdapter);

        mBodyCodeReaderQrv.setOnQRCodeReadListener(this);
        mBodyCodeReaderQrv.setQRDecodingEnabled(true);
        mBodyCodeReaderQrv.setAutofocusInterval(500L);
        //mBodyCodeReaderQrv.setTorchEnabled(true);
        mBodyCodeReaderQrv.setBackCamera();
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r0;
    }

    @Override
    public void sensorSignal(boolean isHigh) {

    }

    @Override
    public void dealIns(byte[] ins) {
        int command = ins[2] & 0xFF;
        switch (command) {
            default: // 其他指令（轮询或失败指令）的处理流程，因为需要操作UI使用Handler传递消息
                mHandler.sendMessage(Message.obtain(mHandler, MSG_RECEIVED_FRAME_FROM_READER, ins));
        }
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
                (byte) MyVars.config.getProductInfoByName(mProductNameSpn.getText().toString()).getCode();
        String bodyCode = mBodyCodeIcl.getCode().substring(3);
        for (int i = 0; i < bodyCode.length(); i++) {
            epc[MyParams.EPC_TYPE_INDEX + 3 + i] = (byte) bodyCode.charAt(i);
        }
        mBucketToRegister = new Bucket(epc);
        // 开始写入任务
        MyVars.executor.execute(new WriteEPCTask());
    }

    private void updateConfigViews() {
        mBodyCodeIcl.setHeader(MyVars.config.getHeader());

        mProductNameSpn.setAdapter(new ArrayAdapter<>(mContext, R.layout.item_common,
                MyVars.config.getProductInfo()));

        String curProductName = mProductNameSpn.getText().toString();
        if (TextUtils.isEmpty(curProductName)
                || MyVars.config.getProductInfoByName(curProductName) == null) {
            if (MyVars.config.getProductInfo() != null && !MyVars.config.getProductInfo().isEmpty()) {
                mProductNameSpn.setText(MyVars.config.getProductInfo().get(0).getName());
            } else {
                mProductNameSpn.setText("");
            }
        }
    }

    private void writeTaskSuccess() {
        mBuckets.add(0, mBucketToRegister);
        mHandler.sendMessage(Message.obtain(mHandler, MSG_SUCCESS));
    }

    private void writeTaskFailed() {
        mHandler.sendMessage(Message.obtain(mHandler, MSG_FAILED));
    }

    private void writeHint(R0Hint hint) {
        Message message = new Message();
        message.what = MSG_UPDATE_HINT;
        message.obj = hint;
        mHandler.sendMessage(message);
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
                case MSG_RECEIVED_FRAME_FROM_READER:
                    byte[] data = (byte[]) msg.obj;
                    outer.mIsUnregisteredEPCRead = false;
                    if (!outer.mIsRegistering && data[2] == 0x22) { // 轮询成功的处理流程
                        int pl = ((data[3] & 0xFF) << 8) + (data[4] & 0xFF);
                        byte[] epc = Arrays.copyOfRange(data, 8, pl + 3);
                        // 判定扫到的EPC是否为前一次扫到的
                        if (Arrays.equals(epc, outer.mScannedEPC)) {
                            outer.mReadCount++;
                        } else {
                            outer.mReadCount = 1;
                            outer.mScannedEPC = epc;
                        }
                        MyParams.EPCType epcType = CommonUtils.validEPC(epc);
                        switch (epcType) {
                            case BUCKET: // 检测到注册桶标签，也允许注册
                            case NONE: // 检测到未注册桶标签，允许注册
                                outer.mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
                                if (outer.mReadCount > CAN_REGISTER_READ_COUNT) {
                                    outer.mIsUnregisteredEPCRead = true;
                                    outer.mRegisterBtn.setEnabled(outer.canRegister());
                                }
                                break;
                            case CARD_ADMIN:
                                outer.mAdminCardScannedCount++;
                                if (outer.mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) { // 启动配置界面，并暂停EPC读取
                                    outer.sendAdminLoginMessage(CommonUtils.bytesToHex(epc));
                                    ConfigActivity.actionStart(outer.getContext());
                                }
                                break;
                        }
                        outer.mReadNoneCount = 0;
                        //outer.mEpcTv.setBackgroundColor(outer.getResources().getColor(R.color.white));
                        //outer.mEpcTv.setText(CommonUtils.bytesToHex(epc));
                        //outer.mEpcTv.setText(CommonUtils.bytesToHex(epc) + epcType.getComment());
                        //outer.mRssiTv.setText(data[5] + "dBm");
                    }
                    else if ((data[2] & 0xFF) == 0xFF){ // 命令帧执行失败的处理流程
                        switch (data[5] & 0xFF) {
                            case 0x15: // 轮询失败
                                outer.mAdminCardScannedCount = 0;
                                if (++outer.mReadNoneCount >= MAX_READ_NONE_COUNT) {
                                    outer.mReadCount = 0;
                                    outer.mScannedEPC = null;
                                    outer.mIsUnregisteredEPCRead = false;
                                    outer.mRegisterBtn.setEnabled(false);
                                    if (!outer.mIsRegistering)
                                        outer.mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                                }
                                //outer.mEpcTv.setBackgroundColor(Color.parseColor(CommonUtils.generateGradientRedColor(outer.mReadNoneCount)));
                                break;
                            case 0x09: // 读时标签不在场区
                            case 0x10: // 写时标签不在场区
                            case 0x16: // Access Password错误
                                Log.i(TAG, "标准错误: " + CommonUtils.bytesToHex(data));
                                break;
                            case 0xA4: // 读Locked
                            case 0xB4: // 写Locked
                                Log.i(TAG, "Locked");
                                break;
                        }
                    }
                    break;
                case MSG_SUCCESS:
                    outer.mAdapter.addRegisteredBucket(outer.mBucketToRegister);
                    outer.mBodyCodeIcl.clear();
                    outer.mIsBodyCodeWritten = false;
                    outer.mIsRegistering = false;
                    outer.mRegisterBtn.setEnabled(false);
                    SpeechSynthesizer.getInstance().speak("注册成功");
                    break;
                case MSG_FAILED:
                    outer.mIsRegistering = false;
                    outer.mRegisterBtn.setEnabled(outer.canRegister());
                    SpeechSynthesizer.getInstance().speak("注册失败");
                    break;
                case MSG_UPDATE_HINT:
                    R0Hint hint = (R0Hint) msg.obj;
                    outer.mHintContentTv.setTextColor(hint.isSuccess ?
                            outer.mContext.getColor(R.color.green) :
                            outer.mContext.getColor(R.color.indian_red));
                    outer.mHintContentTv.setText(hint.content);
                    break;
            }
        }
    }

    private class R0Hint {
        boolean isSuccess;
        String content;

        R0Hint(boolean isSuccess, String content) {
            this.isSuccess = isSuccess;
            this.content = content;
        }
    }

    private class WriteEPCTask implements Runnable {

        @Override
        public void run() {
            byte[] data = null;

            // 这里出现过一个Bug，当桶EPC从16字节变为12字节时，PC的写入及检测流程始终失败，原因在于MASK的设置因为
            // 桶EPC的长度缩减而变失效了
            if (mScannedEPC.length > MyParams.EPC_BUCKET_LENGTH) {
                mScannedEPC = Arrays.copyOf(mScannedEPC, MyParams.EPC_BUCKET_LENGTH);
            }
            try {
                // 设置Mask
                MyVars.getReader().setMask(mScannedEPC);

                // 尝试读取TID
                data = MyVars.getReader().sendCommandSync(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.TID,
                        MyParams.TID_START_INDEX, MyParams.TID_LENGTH), READ_MAX_TRY_COUNT);
                if (data == null) {
                    writeHint(new R0Hint(false, "注册失败,读取TID失败"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "读取TID成功"));
                    mBucketToRegister.setTid(InsHelper.getReadContent(data));
                }

//                // 通过向平台发送TID与桶身码的参数，根据返回结果判定是否重复注册
//                MessageQuery messageQuery = new MessageQuery(CommonUtils.bytesToHex(mBucketToRegister.getTid()),
//                        mBucketToRegister.getBodyCode());
//                Response<Reply> queryResponse = NetHelper.getInstance().checkBodyCodeAndTID(messageQuery).execute();
//                if (!queryResponse.isSuccessful()) {
//                    writeHint(new R0Hint(false, "注册失败,平台连接失败"));
//                    writeTaskFailed();
//                    return;
//                } else if (queryResponse.body() != null && queryResponse.body().getCode() == 210) {
//                    writeHint(new R0Hint(false, "注册失败,标签TID已注册"));
//                    writeTaskFailed();
//                    return;
//                } else if (queryResponse.body() != null && queryResponse.body().getCode() == 211) {
//                    writeHint(new R0Hint(false, "注册失败," + mBucketToRegister.getBodyCode() + "已注册"));
//                    writeTaskFailed();
//                    return;
//                } else {
//                    writeHint(new R0Hint(true, "检验重复注册成功"));
//                }

                // 写入PC
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        1, CommonUtils.hexToBytes(MyParams.BUCKET_PC_CONTENT)), WRITE_PC_MAX_TRY_COUNT);
                // 这里不能检查epc的长度，因为修改PC后返回的EPC是修改之前的长度，而实际长度可能会发生变化
                if (data == null) {
                    writeHint(new R0Hint(false, "注册失败,写入PC失败"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "写入PC成功"));
                }

                // 写入EPC
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, mBucketToRegister.getEpc()), WRITE_EPC_MAX_TRY_COUNT);
                if (data == null) {
                    writeHint(new R0Hint(false, "注册失败,写入EPC失败"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "写入EPC成功"));
                }

                // 尝试上报平台
                MessageRegister message = new MessageRegister(mProductNameSpn.getText().toString());
                message.addBucket(
                        CommonUtils.bytesToHex(mBucketToRegister.getTid()),
                        CommonUtils.bytesToHex(mBucketToRegister.getEpc()),
                        mBucketToRegister.getBodyCode());
                Response<Reply> responseR0 = NetHelper.getInstance().uploadR0Message(message).execute();
                Reply replyR0 = responseR0.body();
                if (!responseR0.isSuccessful()) {
                    writeHint(new R0Hint(false, "注册失败,平台连接失败"));
                    writeTaskFailed();
                    return;
                } else if (replyR0 == null || replyR0.getCode() != 200) {
                    writeHint(new R0Hint(false, "注册失败,桶身码已使用"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "上报平台成功"));
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeHint(new R0Hint(false, "注册失败,网络通信失败"));
                writeTaskFailed();
                return;
            }
            writeHint(new R0Hint(true, mBucketToRegister.getBodyCode() + "注册成功"));
            writeTaskSuccess();
        }
    }
}
