package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.MainActivity;
import com.casc.rfidscanner.activity.R0ConfigActivity;
import com.casc.rfidscanner.activity.R0ListActivity;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.layout.InputCodeLayout;
import com.casc.rfidscanner.message.ConfigChangedMessage;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Response;

/**
 * 桶注册Fragment
 */
public class R0Fragment extends BaseFragment implements InstructionHandler {

    private static final String TAG = R0Fragment.class.getSimpleName();
    private static final int READ_MAX_TRY_COUNT = 5;
    private static final int WRITE_MAX_TRY_COUNT = 3;
    private static final int NETWORK_WAIT_COUNT = 4;
    private static final int CAN_REGISTER_READ_COUNT = 10;
    // Constant for InnerHandler message.what
    private static final int MSG_SUCCESS = 0;
    private static final int MSG_FAILED = 1;
    private static final int MSG_UPDATE_HINT = 2;

    @BindView(R.id.icl_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.btn_register) Button mRegisterBtn;
    @BindView(R.id.iv_tag_status) ImageView mTagStatusIv;
    @BindView(R.id.tv_registered_count) TextView mRegisteredCountTv;
    @BindView(R.id.tv_config_info) TextView mConfigInfoTv;
    @BindView(R.id.tv_r0_hint) TextView mHintContentTv;
    @BindView(R.id.fab_r0_config) FloatingActionButton mConfigFab;

    private String mBucketSpec, mBucketType, mWaterBrand, mWaterSpec,
            mBucketProducer, mBucketOwner, mBucketUser;

    // 已注册桶列表
    private List<Bucket> mBuckets = new ArrayList<>();

    // 注册状态的标志符
    private boolean mIsRegistering;

    // 当前读取的EPC和数据存储区数据
    private byte[] mScannedEPC, mDataRead;

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigChangedMessage message) {
        mBucketSpec = message.bucketSpec;
        mBucketType = message.bucketType;
        mWaterBrand = message.waterBrand;
        mWaterSpec = message.waterSpec;
        mBucketProducer = message.bucketProducer;
        mBucketOwner = message.bucketOwner;
        mBucketUser = message.bucketUser;
        mConfigInfoTv.setText(mBucketSpec + mWaterBrand + mWaterSpec);
    }

    @Override
    protected void initFragment() {
        updateConfigViews();
        MyVars.registeredBuckets = mBuckets;
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        mBodyCodeIcl.setOnInputCompleteListener(new InputCodeLayout.OnInputCompleteCallback() {
            @Override
            public void onInputCompleteListener(String code) {
                mIsBodyCodeWritten = true;
                mRegisterBtn.setEnabled(canRegister());
            }
        });
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r0;
    }

    @Override
    public void deal(byte[] ins) {
        if(D) Log.i(TAG, CommonUtils.bytesToHex(ins));
        int command = ins[2] & 0xFF;
        switch (command) {
            case 0x39: // 读取TID成功的处理流程
                mDataRead = Arrays.copyOfRange(ins, 6 + ins[5], ins.length - 2);
                break;
            case 0x49: // 写入成功的处理流程
                break;
            default: // 其他指令（轮询或失败指令）的处理流程，因为需要操作UI使用Handler传递消息
                mHandler.sendMessage(Message.obtain(mHandler, MSG_RECEIVED_FRAME_FROM_READER, ins));
        }
    }

    @OnClick(R.id.tv_registered_count)
    void onRegisteredCountTextViewClicked() {
        if (ActivityCollector.getTopActivity() instanceof MainActivity) {
            R0ListActivity.actionStart(this.getContext());
        }
    }

    @OnClick(R.id.fab_r0_config)
    void onConfigInfoTextViewClicked() {
        if (ActivityCollector.getTopActivity() instanceof MainActivity) {
            R0ConfigActivity.actionStart(this.getContext(),
                    new String[]{mBucketSpec, mBucketType, mWaterBrand, mWaterSpec,
                            mBucketProducer, mBucketOwner, mBucketUser});
        }
    }

    @OnClick(R.id.btn_register)
    void onRegisterButtonClicked() {
        // 修改注册标志位，禁用界面按钮
        mIsRegistering = true;
        mConfigFab.setEnabled(false);
        mRegisterBtn.setEnabled(false);
        mHintContentTv.setText("");
        // EPC组装，并生成Product实例
        byte[] epc = new byte[16];
        System.arraycopy(CommonUtils.generateEPCHeader(), 0, epc, 0, 5);
        epc[5] = (byte) 0x00;
        System.arraycopy(BigInteger.valueOf(System.currentTimeMillis() / 1000).toByteArray(), 0, epc, 6, 4);
        epc[10] = MyVars.config.getCodeByBucketSpec(mBucketSpec);
        epc[11] = MyVars.config.getCodeByWaterBrand(mWaterBrand);
        epc[12] = MyVars.config.getCodeByWaterSpec(mWaterSpec);
        //epc[13] = MyVars.config.getCodeByBucketType(mBucketType);
        int bodyCode = Integer.valueOf(mBodyCodeIcl.getCode().substring(2));
        epc[13] = (byte) ((bodyCode & 0x00FF0000) >> 16);
        epc[14] = (byte) ((bodyCode & 0x0000FF00) >> 8);
        epc[15] = (byte) (bodyCode & 0x000000FF);
        mBucketToRegister = new Bucket(epc);
        // 开始写入任务
        MyVars.executor.execute(new WriteEPCTask());
    }

    @OnClick({
            R.id.cv_keyboard_one, R.id.cv_keyboard_two, R.id.cv_keyboard_three,
            R.id.cv_keyboard_four, R.id.cv_keyboard_five, R.id.cv_keyboard_six,
            R.id.cv_keyboard_seven, R.id.cv_keyboard_eight, R.id.cv_keyboard_nine,
            R.id.cv_keyboard_zero
    })
    void onKeyboardClicked(CardView view) {
        mVibrator.vibrate(80);
        TextView textView = (TextView) view.getChildAt(0);
        mBodyCodeIcl.addCode(textView.getText().toString());
    }

    @OnClick(R.id.cv_keyboard_clear)
    void onKeyboardClearClicked() {
        mVibrator.vibrate(80);
        mBodyCodeIcl.clear();
        mIsBodyCodeWritten = false;
        mRegisterBtn.setEnabled(false);
    }

    @OnClick(R.id.cv_keyboard_back)
    void onKeyboardBackClicked() {
        mVibrator.vibrate(80);
        mBodyCodeIcl.deleteCode();
        mIsBodyCodeWritten = false;
        mRegisterBtn.setEnabled(false);
    }

    private void updateConfigViews() {
        mBodyCodeIcl.setHeader(MyVars.config.getCompanySymbol());

        try {
            MyVars.config.getCodeByBucketSpec(mBucketSpec);
        } catch (Exception e) {
            mBucketSpec = MyVars.config.getBucketSpecInfo().isEmpty() ? null :
                    MyVars.config.getBucketSpecInfo().get(0).getSpecify();
        }

        try {
            MyVars.config.getCodeByBucketType(mBucketType);
        } catch (Exception e) {
            mBucketType = MyVars.config.getBucketTypeInfo().isEmpty() ? null :
                    MyVars.config.getBucketTypeInfo().get(0).getSpecify();
        }

        try {
            MyVars.config.getCodeByWaterBrand(mWaterBrand);
        } catch (Exception e) {
            mWaterBrand = MyVars.config.getWaterBrandInfo().isEmpty() ? null :
                    MyVars.config.getWaterBrandInfo().get(0).getSpecify();
        }

        try {
            MyVars.config.getCodeByWaterSpec(mWaterSpec);
        } catch (Exception e) {
            mWaterSpec = MyVars.config.getWaterSpecInfo().isEmpty() ? null :
                    MyVars.config.getWaterSpecInfo().get(0).getSpecify();
        }

        try {
            MyVars.config.getCodeByBucketProducer(mBucketProducer);
        } catch (Exception e) {
            mBucketProducer = MyVars.config.getBucketProducerInfo().isEmpty() ? null :
                    MyVars.config.getBucketProducerInfo().get(0).getSpecify();
        }

        try {
            MyVars.config.getCodeByBucketOwner(mBucketOwner);
        } catch (Exception e) {
            mBucketOwner = MyVars.config.getBucketOwnerInfo().isEmpty() ? null :
                    MyVars.config.getBucketOwnerInfo().get(0).getSpecify();
        }

        try {
            MyVars.config.getCodeByBucketUser(mBucketUser);
        } catch (Exception e) {
            mBucketUser = MyVars.config.getBucketUserInfo().isEmpty() ? null :
                    MyVars.config.getBucketUserInfo().get(0).getSpecify();
        }

        mConfigInfoTv.setText(mBucketSpec + mWaterBrand + mWaterSpec);
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
                            outer.mReadCount = 0;
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
                                outer.mReadNoneCount += 8;
                                if (outer.mReadNoneCount > 8) {
                                    outer.mReadCount = 0;
                                    outer.mIsUnregisteredEPCRead = false;
                                    outer.mRegisterBtn.setEnabled(false);
                                }
                                outer.mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                                //outer.mEpcTv.setBackgroundColor(Color.parseColor(CommonUtils.generateGradientRedColor(outer.mReadNoneCount)));
                                break;
                            case 0x09: // 读时标签不在场区
                            case 0x10: // 写时标签不在场区
                            case 0x16: // Access Password错误
                                Log.i(TAG, "标准错误");
                                Log.i(TAG, CommonUtils.bytesToHex(data));
                                break;
                            case 0xA4: // 读Locked
                            case 0xB4: // 写Locked
                                Log.i(TAG, "Locked");
                                break;
                        }
                    }
                    break;
                case MSG_SUCCESS:
                    int bodyCode = Integer.valueOf(outer.mBodyCodeIcl.getCode().substring(2));
                    outer.mBodyCodeIcl.setCode(String.format("%06d", ++bodyCode));
                    outer.mIsRegistering = false;
                    outer.mRegisteredCountTv.setText(String.valueOf(outer.mBuckets.size()));
                    outer.mRegisterBtn.setEnabled(false);
                    outer.mConfigFab.setEnabled(true);
                    SpeechSynthesizer.getInstance().speak("注册成功");
                    break;
                case MSG_FAILED:
                    outer.mIsRegistering = false;
                    outer.mRegisterBtn.setEnabled(outer.canRegister());
                    outer.mConfigFab.setEnabled(true);
                    SpeechSynthesizer.getInstance().speak("注册失败");
                    break;
                case MSG_UPDATE_HINT:
                    R0Hint hint = (R0Hint) msg.obj;
                    outer.mHintContentTv.setTextColor(hint.isSuccess ?
                            MyApplication.getInstance().getColor(R.color.green) :
                            MyApplication.getInstance().getColor(R.color.indian_red));
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
            if (mScannedEPC.length > 14) {
                mScannedEPC = Arrays.copyOf(mScannedEPC, 14);
            }
            try {
                // 设置Mask
                MyVars.getReader().sendCommand(
                        InsHelper.getEPCSelectParameter(mScannedEPC), MyParams.SELECT_MAX_TRY_COUNT);

                // 尝试读取TID
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.TID,
                        MyParams.TID_START_INDEX, MyParams.TID_LENGTH), READ_MAX_TRY_COUNT);
                Thread.sleep((READ_MAX_TRY_COUNT + 1) * LinkType.getSendInterval());
                if (mDataRead == null) {
                    writeHint(new R0Hint(false, "注册失败,读取TID失败"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "读取TID成功"));
                    mBucketToRegister.setTid(mDataRead);
                }

                // 通过向平台发送TID与桶身码的参数，根据返回结果判定是否重复注册
                MessageQuery messageQuery = new MessageQuery(CommonUtils.bytesToHex(mBucketToRegister.getTid()),
                        mBucketToRegister.getBodyCode());
                Response<Reply> queryResponse = NetHelper.getInstance().checkBodyCodeAndTID(messageQuery).execute();
                Thread.sleep(NETWORK_WAIT_COUNT * LinkType.getSendInterval());
                if (!queryResponse.isSuccessful()) {
                    writeHint(new R0Hint(false, "注册失败,平台连接失败"));
                    writeTaskFailed();
                    return;
                } else if (queryResponse.body() != null && queryResponse.body().getCode() == 210) {
                    writeHint(new R0Hint(false, "注册失败,TID已注册"));
                    writeTaskFailed();
                    return;
                } else if (queryResponse.body() != null && queryResponse.body().getCode() == 211) {
                    writeHint(new R0Hint(false, "注册失败,桶身码已注册"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "检验重复注册成功"));
                }

                // 写入PC
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        1, CommonUtils.hexToBytes(MyParams.BUCKET_PC_CONTENT)), WRITE_MAX_TRY_COUNT);
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        1, 1), READ_MAX_TRY_COUNT);
                Thread.sleep((WRITE_MAX_TRY_COUNT + READ_MAX_TRY_COUNT + 1) * LinkType.getSendInterval());
                if (mDataRead == null || ((mDataRead[0] & 0xFF) >> 3) != mBucketToRegister.getEpc().length / 2) {
                    writeHint(new R0Hint(false, "注册失败,写入PC失败"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "写入PC成功"));
                }

                // 写入EPC
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, mBucketToRegister.getEpc()), WRITE_MAX_TRY_COUNT);
                MyVars.getReader().sendCommand(
                        InsHelper.getEPCSelectParameter(mBucketToRegister.getEpc()), 2);
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, MyParams.EPC_BUCKET_LENGTH / 2), READ_MAX_TRY_COUNT);
                Thread.sleep((WRITE_MAX_TRY_COUNT + READ_MAX_TRY_COUNT + 3) * LinkType.getSendInterval());
                if (!Arrays.equals(mDataRead, mBucketToRegister.getEpc())) {
                    writeHint(new R0Hint(false, "注册失败,写入EPC失败"));
                    writeTaskFailed();
                    return;
                } else {
                    writeHint(new R0Hint(true, "写入EPC成功"));
                }

                // 尝试上报平台
                MessageRegister message = new MessageRegister();
                message.setBucketspec(mBucketSpec);
                message.setBuckettype(mBucketType);
                message.setWaterbrand(mWaterBrand);
                message.setWaterspec(mWaterSpec);
                message.setBucketproducer(mBucketProducer);
                message.setBucketowner(mBucketOwner);
                message.setBucketuser(mBucketUser);
                message.addBucketInfo(
                        CommonUtils.bytesToHex(mBucketToRegister.getTid()),
                        CommonUtils.bytesToHex(mBucketToRegister.getEpc()),
                        mBucketToRegister.getBodyCode());
                Response<Reply> responseR0 = NetHelper.getInstance().uploadR0Message(message).execute();
                Thread.sleep(NETWORK_WAIT_COUNT * LinkType.getSendInterval());
                if (!responseR0.isSuccessful()) {
                    Log.i(TAG, responseR0.toString());
                    writeHint(new R0Hint(false, "注册失败,平台连接失败"));
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
