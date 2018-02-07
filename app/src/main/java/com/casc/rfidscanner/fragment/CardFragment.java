package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.CardAdapter;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.bean.Card;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageCardReg;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.layout.InputCodeLayout;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.CommonUtils;
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

public class CardFragment extends BaseFragment implements InstructionHandler {

    private static final String TAG = CardFragment.class.getSimpleName();
    private static final int READ_MAX_TRY_COUNT = 5;
    private static final int WRITE_MAX_TRY_COUNT = 3;
    private static final int CAN_REGISTER_READ_COUNT = 20;
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_HINT = 0;
    private static final int MSG_SUCCESS = 1;
    private static final int MSG_FAILED = 2;

    @BindView(R.id.spn_card_type) BetterSpinner mCardTypeSpn;
    @BindView(R.id.spn_card_validity) BetterSpinner mCardValiditySpn;

    @BindView(R.id.tv_epc) TextView mEpcTv;
    @BindView(R.id.tv_rssi) TextView mRssiTv;

    @BindView(R.id.icl_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.btn_register) Button mRegisterBtn;

    @BindView(R.id.rv_card_list) RecyclerView mCardRv;
    @BindView(R.id.rv_card_hint_list) RecyclerView mHintRv;

    // 已注册卡列表
    private List<Card> mCards = new ArrayList<>();

    // 提示消息列表
    private List<Hint> mHints = new ArrayList<>();

    // 已注册桶列表适配器
    private CardAdapter mCardAdapter;

    // 提示消息列表适配器
    private HintAdapter mHintAdapter;

    // 注册状态的标志符
    private boolean mIsRegistering;

    // 当前读取的EPC和数据存储区数据
    private byte[] mScannedEPC, mDataRead;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mReadNoneCount;

    // 注册所必需的相关元素标志位
    private boolean mIsUnregisteredEPCRead, mIsAllConnectionsReady, mIsBodyCodeWritten;

    // 要注册的桶实例
    private Card mCardToRegister;

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
        }
        else {
            mIsAllConnectionsReady = false;
            mRegisterBtn.setEnabled(false);
        }
    }

    @Override
    protected void initFragment() {
        mCardAdapter = new CardAdapter(mCards);
        mHintAdapter = new HintAdapter(mHints);
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        mCardTypeSpn.setText(getResources().getStringArray(R.array.card_type)[0]);
        mCardTypeSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, getResources().getStringArray(R.array.card_type)));
        mCardTypeSpn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                switch (s.toString()) {
                    case "出库专用卡":
                        mBodyCodeIcl.setHeader(MyVars.config.getCompanySymbol() + "C");
                        break;
                    case "运维专用卡":
                        mBodyCodeIcl.setHeader("PTA");
                        break;
                    case "回流专用卡":
                        mBodyCodeIcl.setHeader(MyVars.config.getCompanySymbol() + "R");
                        break;
                }
            }
        });
        mCardValiditySpn.setText(getResources().getStringArray(R.array.card_validity)[0]);
        mCardValiditySpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, getResources().getStringArray(R.array.card_validity)));

        mBodyCodeIcl.setOnInputCompleteListener(new InputCodeLayout.OnInputCompleteCallback() {
            @Override
            public void onInputCompleteListener(String code) {
                mIsBodyCodeWritten = true;
                mRegisterBtn.setEnabled(canRegister());
            }
        });
        mCardRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mCardRv.setAdapter(mCardAdapter);
        mHintRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mHintRv.setAdapter(mHintAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_card;
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

    @OnClick(R.id.btn_register)
    void onRegisterButtonClicked() {
        // 清空提示区，已便本次注册使用
        mHints.clear();
        mHintAdapter.notifyDataSetChanged();
//        // 简单检查是否与刚注册过的重复
//        for (Card card : mCards) {
//            if (card.getBodyCode().equals(mBodyCodeIcl.getCode())) {
//                writeHint("请勿重复注册");
//                return;
//            }
//        }
        // 修改注册标志位，禁用界面按钮
        mIsRegistering = true;
        mRegisterBtn.setEnabled(false);
        // 生成Card实例
        mCardToRegister = new Card(mCardTypeSpn.getText().toString(), mBodyCodeIcl.getCode(), mCardValiditySpn.getText().toString());
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
        mVibrator.vibrate(30);
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
        mVibrator.vibrate(50);
        mBodyCodeIcl.deleteCode();
        mIsBodyCodeWritten = false;
        mRegisterBtn.setEnabled(false);
    }

    @OnClick(R.id.btn_card_back)
    void onCardBackClicked() {
         MyVars.getReader().pause();
         ConfigActivity.actionStart(getContext());
    }

    private void writeTaskSuccess() {
        mCards.add(0, mCardToRegister);
        mHandler.sendMessage(Message.obtain(mHandler, MSG_SUCCESS));
    }

    private void writeTaskFailed() {
        mHandler.sendMessage(Message.obtain(mHandler, MSG_FAILED));
    }

    private void writeHint(String content) {
        mHints.add(0, new Hint(content));
        mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_HINT));
    }

    private boolean canRegister() {
        return MyVars.getReader().isConnected() & !mIsRegistering &&
                mIsAllConnectionsReady && mIsUnregisteredEPCRead && mIsBodyCodeWritten;
    }

    private static class InnerHandler extends Handler {

        private WeakReference<CardFragment> mOuter;

        InnerHandler(CardFragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            CardFragment outer = mOuter.get();
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
                        }
                        else {
                            outer.mReadCount = 0;
                            outer.mScannedEPC = epc;
                        }
                        if (outer.mReadCount > CAN_REGISTER_READ_COUNT) {
                            outer.mIsUnregisteredEPCRead = true;
                            outer.mRegisterBtn.setEnabled(outer.canRegister());
                        }
                        outer.mReadNoneCount = 0;
                        outer.mEpcTv.setBackgroundColor(outer.getResources().getColor(R.color.white));
                        //outer.mEpcTv.setText(CommonUtils.bytesToHex(epc));
                        outer.mEpcTv.setText(CommonUtils.bytesToHex(epc) + CommonUtils.validEPC(epc).getComment());
                        outer.mRssiTv.setText(data[5] + "dBm");
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
                                outer.mEpcTv.setBackgroundColor(Color.parseColor(CommonUtils.generateGradientRedColor(outer.mReadNoneCount)));
                                break;
                            case 0x09: // 读时标签不在场区
                            case 0x10: // 写时标签不在场区
                            case 0x16: // Access Password错误
                                Log.i(TAG, "标准错误");
                                break;
                            case 0xA4: // 读Locked
                            case 0xB4: // 写Locked
                                Log.i(TAG, "Locked");
                                break;
                        }
                    }
                    break;
                case MSG_UPDATE_HINT:
                    outer.mHintAdapter.notifyDataSetChanged();
                    break;
                case MSG_SUCCESS:
                    outer.mIsRegistering = false;
                    outer.mIsBodyCodeWritten = false;
                    outer.mBodyCodeIcl.clear();
                    outer.mCardAdapter.notifyDataSetChanged();
                    outer.mRegisterBtn.setEnabled(false);
                    outer.playSound(0, 1);
                    break;
                case MSG_FAILED:
                    outer.mIsRegistering = false;
                    outer.mRegisterBtn.setEnabled(outer.canRegister());
                    break;
            }
        }
    }

    private class WriteEPCTask implements Runnable {

        @Override
        public void run() {
            if (Integer.valueOf(mBodyCodeIcl.getCode().substring(3)) > 255) {
                writeHint("可视码最大编号为255");
                writeHint("注册失败");
                writeTaskFailed();
                return;
            }
            if (mScannedEPC.length > 12) {
                mScannedEPC = Arrays.copyOf(mScannedEPC, 12);
            }
            try {
                // 设置Mask
                MyVars.getReader().sendCommand(
                        InsHelper.getEPCSelectParameter(mScannedEPC), MyParams.SELECT_MAX_TRY_COUNT);

                // 尝试读取TID
                writeHint("读取TID");
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.TID,
                        MyParams.TID_START_INDEX, MyParams.TID_LENGTH), READ_MAX_TRY_COUNT);
                Thread.sleep((READ_MAX_TRY_COUNT + 1) * LinkType.getSendInterval());
                if (mDataRead == null) {
                    writeHint("读取TID失败");
                    writeTaskFailed();
                    return;
                } else {
                    mCardToRegister.setTid(mDataRead);
                }

                // 写入PC
                writeHint("写入PC");
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        1, mCardToRegister.getPc()), WRITE_MAX_TRY_COUNT);
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        1, 1), READ_MAX_TRY_COUNT);
                Thread.sleep((WRITE_MAX_TRY_COUNT + READ_MAX_TRY_COUNT + 1) * LinkType.getSendInterval());
                if (mDataRead == null || ((mDataRead[0] & 0xFF) >> 3) != mCardToRegister.getEpc().length / 2) {
                    writeHint("写入PC失败");
                    writeTaskFailed();
                    return;
                }

                // 写入EPC
                writeHint("写入EPC");
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, mCardToRegister.getEpc()), WRITE_MAX_TRY_COUNT);
                MyVars.getReader().sendCommand(
                        InsHelper.getEPCSelectParameter(mCardToRegister.getEpc()), 2);
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, mCardToRegister.getEpc().length / 2), READ_MAX_TRY_COUNT);
                Thread.sleep((WRITE_MAX_TRY_COUNT + READ_MAX_TRY_COUNT + 3) * LinkType.getSendInterval());
                if (!Arrays.equals(mDataRead, mCardToRegister.getEpc())) {
                    writeHint("写入EPC失败");
                    writeTaskFailed();
                    return;
                }

                // 尝试上报平台
                writeHint("上报平台");
                MessageCardReg message = new MessageCardReg(mCardToRegister);
                Response<Reply> responseCardReg = NetHelper.getInstance().uploadCardRegMessage(message).execute();
                if (!responseCardReg.isSuccessful()) {
                    writeHint("平台连接失败");
                    writeTaskFailed();
                    return;
                }
                else if (responseCardReg.body() != null && responseCardReg.body().getCode() == 210) {
                    writeHint("TID已注册");
                    writeTaskFailed();
                    return;
                }
                else if (responseCardReg.body() != null && responseCardReg.body().getCode() == 211) {
                    writeHint("可视码已注册");
                    writeTaskFailed();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeHint("网络通信失败");
                writeTaskFailed();
                return;
            }
            writeHint(mCardToRegister.getBodyCode() + " 注册成功");
            writeTaskSuccess();
        }
    }
}