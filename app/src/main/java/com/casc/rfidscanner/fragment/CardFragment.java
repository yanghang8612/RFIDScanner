package com.casc.rfidscanner.fragment;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.CardAdapter;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.bean.Card;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MsgCardReg;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.InputCodeLayout;
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
 * 专用卡注册Fragment
 */
public class CardFragment extends BaseFragment {

    private static final String TAG = CardFragment.class.getSimpleName();
    private static final int CAN_REGISTER_READ_COUNT = 20;
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_HINT = 0;
    private static final int MSG_SUCCESS = 1;
    private static final int MSG_FAILED = 2;

    @BindView(R.id.spn_card_type) BetterSpinner mCardTypeSpn;
    @BindView(R.id.ll_card_special) LinearLayout mCardSpecialLl;
    @BindView(R.id.sw_card_special) Switch mCardSpecialSw;
    @BindView(R.id.spn_card_validity) BetterSpinner mCardValiditySpn;

    @BindView(R.id.tv_epc) TextView mEpcTv;
    @BindView(R.id.tv_rssi) TextView mRssiTv;

    @BindView(R.id.icl_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.btn_card_register) Button mRegisterBtn;

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

    // 当前读取的EPC
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mReadNoneCount;

    // 注册所必需的相关元素标志位
    private boolean mIsUnregisteredEPCRead, mIsAllConnectionsReady, mIsBodyCodeWritten;

    // 要注册的桶实例
    private Card mCardToRegister;

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
    public void onMessageEvent(PollingResultMessage message) {
        mIsUnregisteredEPCRead = false;
        if (message.isRead) {
            // 判定扫到的EPC是否为前一次扫到的
            if (Arrays.equals(message.epc, mScannedEPC)) {
                mReadCount++;
            } else {
                mReadCount = 0;
                mScannedEPC = message.epc;
            }
            if (mReadCount > CAN_REGISTER_READ_COUNT) {
                mIsUnregisteredEPCRead = true;
                mRegisterBtn.setEnabled(canRegister());
            }
            mReadNoneCount = 0;
            mEpcTv.setBackgroundColor(mContext.getColor(R.color.white));
            //outer.mEpcTv.setText(CommonUtils.bytesToHex(epc));
            mEpcTv.setText(CommonUtils.bytesToHex(mScannedEPC)
                    + CommonUtils.validEPC(mScannedEPC).getComment());
        } else {
            mReadNoneCount += 8;
            if (mReadNoneCount > 8) {
                mReadCount = 0;
                mIsUnregisteredEPCRead = false;
                mRegisterBtn.setEnabled(false);
            }
            mEpcTv.setBackgroundColor(
                    Color.parseColor(CommonUtils.generateGradientRedColor(mReadNoneCount)));
        }
    }

    @Override
    protected void initFragment() {
        mCardAdapter = new CardAdapter(mCards);
        mHintAdapter = new HintAdapter(mHints);

        mBodyCodeIcl.setHeader(MyVars.config.getCompanySymbol() + "C");
        mCardTypeSpn.setText(getResources().getStringArray(R.array.card_type)[0]);
        mCardTypeSpn.setAdapter(new ArrayAdapter<>(mContext, R.layout.item_specify,
                getResources().getStringArray(R.array.card_type)));
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
                        mCardSpecialLl.setVisibility(View.VISIBLE);
                        break;
                    case "运维专用卡":
                        mBodyCodeIcl.setHeader("PTA");
                        mCardSpecialLl.setVisibility(View.GONE);
                        break;
                    case "回流专用卡":
                        mBodyCodeIcl.setHeader(MyVars.config.getCompanySymbol() + "R");
                        mCardSpecialLl.setVisibility(View.GONE);
                        break;
                }
                mCardSpecialSw.setChecked(false);
            }
        });
        mCardValiditySpn.setText(getResources().getStringArray(R.array.card_validity)[0]);
        mCardValiditySpn.setAdapter(new ArrayAdapter<>(mContext, R.layout.item_specify,
                getResources().getStringArray(R.array.card_validity)));

        mBodyCodeIcl.setOnInputCompleteListener(new InputCodeLayout.OnInputCompleteCallback() {
            @Override
            public void onInputCompleteListener(String code) {
                mIsBodyCodeWritten = true;
                mRegisterBtn.setEnabled(canRegister());
            }
        });
        mCardRv.setLayoutManager(new LinearLayoutManager(mContext));
        mCardRv.setAdapter(mCardAdapter);
        mHintRv.setLayoutManager(new LinearLayoutManager(mContext));
        mHintRv.setAdapter(mHintAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_card;
    }

    @OnClick(R.id.btn_card_register)
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
        if (mCardSpecialSw.isChecked()) mCardToRegister.setSpecial();
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
         ConfigActivity.actionStart(mContext);
    }

    private void writeTaskSuccess() {
        mCards.add(0, mCardToRegister);
        Message.obtain(mHandler, MSG_SUCCESS).sendToTarget();
    }

    private void writeTaskFailed() {
        Message.obtain(mHandler, MSG_FAILED).sendToTarget();
    }

    private void writeHint(String content) {
        mHints.add(0, new Hint(content));
        Message.obtain(mHandler, MSG_UPDATE_HINT).sendToTarget();
    }

    private boolean canRegister() {
        return !mIsRegistering && mIsAllConnectionsReady
                && mIsUnregisteredEPCRead && mIsBodyCodeWritten;
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
                case MSG_UPDATE_HINT:
                    outer.mHintAdapter.notifyDataSetChanged();
                    break;
                case MSG_SUCCESS:
                    outer.mIsRegistering = false;
                    outer.mIsBodyCodeWritten = false;
                    outer.mBodyCodeIcl.clear();
                    outer.mCardAdapter.notifyDataSetChanged();
                    outer.mRegisterBtn.setEnabled(false);
                    outer.playSound();
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
            byte[] data;

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
                MyVars.getReader().setMask(mScannedEPC, 2);

                // 尝试读取TID
                writeHint("读取TID");
                data = MyVars.getReader().sendCommandSync(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.TID,
                        MyParams.TID_START_INDEX,
                        MyParams.TID_READ_LENGTH));
                if (data == null) {
                    writeHint("读取TID失败");
                    writeTaskFailed();
                    return;
                } else {
                    mCardToRegister.setTid(InsHelper.getReadContent(data));
                }

                // 写入PC
                writeHint("写入PC");
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        1,
                        mCardToRegister.getPc()));
                if (data != null) {
                    writeHint("写入PC成功");
                } else {
                    writeHint("写入PC失败");
                    writeTaskFailed();
                    return;
                }

                // 写入EPC
                writeHint("写入EPC");
                data = MyVars.getReader().sendCommandSync(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        2,
                        mCardToRegister.getEpc()));
                if (data == null) {
                    writeHint("写入EPC失败");
                    writeTaskFailed();
                    return;
                } else {
                    writeHint("写入EPC成功");
                }

                // 尝试上报平台
                writeHint("上报平台");
                MsgCardReg msg = new MsgCardReg(mCardToRegister);
                Response<Reply> responseCardReg = NetHelper.getInstance().uploadCardRegMsg(msg).execute();
                Reply replyCardReg = responseCardReg.body();
                if (!responseCardReg.isSuccessful()) {
                    writeHint("平台连接失败");
                    writeTaskFailed();
                    return;
                } else if (replyCardReg != null && replyCardReg.getCode() == 210) {
                    writeHint("TID已注册");
                    writeTaskFailed();
                    return;
                } else if (replyCardReg != null && replyCardReg.getCode() == 211) {
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
