package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.adapter.ProductAdapter;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.bean.Product;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.layout.InputCodeLayout;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.suke.widget.SwitchButton;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import at.markushi.ui.CircleButton;
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
    private static final int CAN_REGISTER_READ_COUNT = 10;
    // Constant for InnerHandler message.what
    private static final int MSG_SWITCH_CARD_VISIBILITY = 0;
    private static final int MSG_UPDATE_HINT = 1;
    private static final int MSG_SUCCESS = 2;
    private static final int MSG_FAILED = 3;

    @BindView(R.id.btn_modify) CircleButton mModifyBtn;
    @BindView(R.id.btn_cancel) CircleButton mCancelBtn;
    @BindView(R.id.btn_save) CircleButton mSaveBtn;

    @BindView(R.id.tv_product_bucket_spec) TextView mBucketSpecTv;
    @BindView(R.id.spn_bucket_spec) BetterSpinner mBucketSpecSpn;

    @BindView(R.id.tv_bucket_type) TextView mBucketTypeTv;
    @BindView(R.id.spn_bucket_type) BetterSpinner mBucketTypeSpn;

    @BindView(R.id.tv_product_water_brand) TextView mWaterBrandTv;
    @BindView(R.id.spn_water_brand) BetterSpinner mWaterBrandSpn;

    @BindView(R.id.tv_product_water_spec) TextView mWaterSpecTv;
    @BindView(R.id.spn_water_spec) BetterSpinner mWaterSpecSpn;

    @BindView(R.id.tv_bucket_producer) TextView mBucketProducerTv;
    @BindView(R.id.spn_bucket_producer) BetterSpinner mBucketProducerSpn;

    @BindView(R.id.tv_bucket_owner) TextView mBucketOwnerTv;
    @BindView(R.id.spn_bucket_owner) BetterSpinner mBucketOwnerSpn;

    @BindView(R.id.tv_bucket_user) TextView mBucketUserTv;
    @BindView(R.id.spn_bucket_user) BetterSpinner mBucketUserSpn;

    @BindView(R.id.tv_epc) TextView mEpcTv;
    @BindView(R.id.tv_rssi) TextView mRssiTv;
    @BindView(R.id.tv_registered_count) TextView mRegisteredCountTv;

    @BindView(R.id.icl_body_code) InputCodeLayout mBodyCodeIcl;
    @BindView(R.id.sbtn_auto_complete) SwitchButton mAutoCompleteSbtn;
    @BindView(R.id.btn_register) Button mRegisterBtn;

    @BindView(R.id.rv_registered_list) RecyclerView mRegisteredRv;
    @BindView(R.id.rv_r0_hint_list) RecyclerView mHintRv;

    // 已注册桶列表
    private List<Product> mProducts = new ArrayList<>();

    // 提示消息列表
    private List<Hint> mHints = new ArrayList<>();

    // 已注册桶列表适配器
    private ProductAdapter mRegisteredAdapter;

    // 提示消息列表适配器
    private HintAdapter mHintAdapter;

    // 编辑状态和注册状态的标志符
    private boolean mIsEditing, mIsRegistering;

    // 当前读取的EPC和数据存储区数据
    private byte[] mScannedEPC, mDataRead;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mReadNoneCount;

    // 注册所必需的相关元素标志位
    private boolean mIsUnregisteredEPCRead, mIsAllConnectionsReady, mIsBodyCodeWritten;

    // 要注册的桶实例
    private Product mProductToRegister;

    // 系统震动辅助类
    private Vibrator mVibrator;

    // 自动填写标志位
    private boolean isAutoComplete;

    // 自动填写的桶身码
    private int mBodyCode;

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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        super.onMessageEvent(message);
        updateConfigViews();
    }

    @Override
    protected void initFragment() {
        initConfigViews();
        updateConfigViews();
        mRegisteredAdapter = new ProductAdapter(mProducts);
        mHintAdapter = new HintAdapter(mHints);
        mVibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        mBodyCodeIcl.setOnInputCompleteListener(new InputCodeLayout.OnInputCompleteCallback() {
            @Override
            public void onInputCompleteListener(String code) {
                mIsBodyCodeWritten = true;
                mRegisterBtn.setEnabled(canRegister());
            }
        });
        mRegisteredRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mRegisteredRv.setAdapter(mRegisteredAdapter);
        mHintRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mHintRv.setAdapter(mHintAdapter);
        mBodyCode = Integer.valueOf(ConfigHelper.getParam(MyParams.S_CODE));
        mAutoCompleteSbtn.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
            if (isChecked)  {
                mBodyCodeIcl.setCode(String.format("%06d", mBodyCode));
            }
            else {
                mBodyCodeIcl.clear();
            }
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

    @OnClick(R.id.btn_modify)
    void onModifyButtonClicked() {
        mIsEditing = true;
        mRegisterBtn.setEnabled(false);
        mBucketSpecSpn.setText(mBucketSpecTv.getText());
        mBucketTypeSpn.setText(mBucketTypeTv.getText());
        mWaterBrandSpn.setText(mWaterBrandTv.getText());
        mWaterSpecSpn.setText(mWaterSpecTv.getText());
        mBucketProducerSpn.setText(mBucketProducerTv.getText());
        mBucketOwnerSpn.setText(mBucketOwnerTv.getText());
        mBucketUserSpn.setText(mBucketUserTv.getText());
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_SWITCH_CARD_VISIBILITY), 100);
    }

    @OnClick(R.id.btn_cancel)
    void onCancelButtonClicked() {
        mIsEditing = false;
        mRegisterBtn.setEnabled(canRegister());
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_SWITCH_CARD_VISIBILITY), 100);
    }

    @OnClick(R.id.btn_save)
    void onSaveButtonClicked() {
        mIsEditing = false;
        mRegisterBtn.setEnabled(canRegister());
        mBucketSpecTv.setText(mBucketSpecSpn.getText());
        mBucketTypeTv.setText(mBucketTypeSpn.getText());
        mWaterBrandTv.setText(mWaterBrandSpn.getText());
        mWaterSpecTv.setText(mWaterSpecSpn.getText());
        mBucketProducerTv.setText(mBucketProducerSpn.getText());
        mBucketOwnerTv.setText(mBucketOwnerSpn.getText());
        mBucketUserTv.setText(mBucketUserSpn.getText());
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_SWITCH_CARD_VISIBILITY), 100);
    }

    @OnClick(R.id.btn_register)
    void onRegisterButtonClicked() {
        // 清空提示区，已便本次注册使用
        mHints.clear();
        mHintAdapter.notifyDataSetChanged();
//        // 简单检查是否与刚注册过的重复
//        for (Product p : mProducts) {
//            if (p.getBodyCode().equals(mBodyCodeIcl.getCode())) {
//                writeHint("请勿重复注册");
//                return;
//            }
//        }
        // 修改注册标志位，禁用界面按钮
        mIsRegistering = true;
        mModifyBtn.setEnabled(false);
        mAutoCompleteSbtn.setEnabled(false);
        mRegisterBtn.setEnabled(false);
        // EPC组装，并生成Product实例
        byte[] epc = new byte[16];
        System.arraycopy(CommonUtils.generateEPCHeader(), 0, epc, 0, 5);
        epc[5] = (byte) 0x00;
        System.arraycopy(BigInteger.valueOf(System.currentTimeMillis() / 1000).toByteArray(), 0, epc, 6, 4);
        epc[10] = MyVars.config.getCodeByBucketSpec(mBucketSpecTv.getText().toString());
        epc[11] = MyVars.config.getCodeByWaterBrand(mWaterBrandTv.getText().toString());
        epc[12] = MyVars.config.getCodeByWaterSpec(mWaterSpecTv.getText().toString());
        //epc[13] = MyVars.config.getCodeByBucketType(mBucketTypeTv.getText().toString());
        int bodyCode = Integer.valueOf(mBodyCodeIcl.getCode().substring(2));
        epc[13] = (byte) ((bodyCode & 0x00FF0000) >> 16);
        epc[14] = (byte) ((bodyCode & 0x0000FF00) >> 8);
        epc[15] = (byte) (bodyCode & 0x000000FF);
        mProductToRegister = new Product(epc);
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
        if (mAutoCompleteSbtn.isChecked()) return;
        mVibrator.vibrate(30);
        TextView textView = (TextView) view.getChildAt(0);
        mBodyCodeIcl.addCode(textView.getText().toString());
    }

    @OnClick(R.id.cv_keyboard_clear)
    void onKeyboardClearClicked() {
        if (mAutoCompleteSbtn.isChecked()) return;
        mVibrator.vibrate(80);
        mBodyCodeIcl.clear();
        mIsBodyCodeWritten = false;
        mRegisterBtn.setEnabled(false);
    }

    @OnClick(R.id.cv_keyboard_back)
    void onKeyboardBackClicked() {
        if (mAutoCompleteSbtn.isChecked()) return;
        mVibrator.vibrate(50);
        mBodyCodeIcl.deleteCode();
        mIsBodyCodeWritten = false;
        mRegisterBtn.setEnabled(false);
    }

    private void initConfigViews() {
        if (!MyVars.config.getBucketSpecInfo().isEmpty())
            mBucketSpecTv.setText(MyVars.config.getBucketSpecInfo().get(0).getSpecify());

        if (!MyVars.config.getBucketTypeInfo().isEmpty())
            mBucketTypeTv.setText(MyVars.config.getBucketTypeInfo().get(0).getSpecify());

        if (!MyVars.config.getWaterBrandInfo().isEmpty())
            mWaterBrandTv.setText(MyVars.config.getWaterBrandInfo().get(0).getSpecify());

        if (!MyVars.config.getWaterSpecInfo().isEmpty())
            mWaterSpecTv.setText(MyVars.config.getWaterSpecInfo().get(0).getSpecify());

        if (!MyVars.config.getBucketProducerInfo().isEmpty())
            mBucketProducerTv.setText(MyVars.config.getBucketProducerInfo().get(0).getSpecify());

        if (!MyVars.config.getBucketOwnerInfo().isEmpty())
            mBucketOwnerTv.setText(MyVars.config.getBucketOwnerInfo().get(0).getSpecify());

        if (!MyVars.config.getBucketUserInfo().isEmpty())
            mBucketUserTv.setText(MyVars.config.getBucketUserInfo().get(0).getSpecify());
    }

    private void updateConfigViews() {
        mBodyCodeIcl.setHeader(MyVars.config.getCompanySymbol());
        try {
            MyVars.config.getCodeByBucketSpec(mBucketSpecTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getBucketSpecInfo().isEmpty())
                mBucketSpecTv.setText(MyVars.config.getBucketSpecInfo().get(0).getSpecify());
            else
                mBucketSpecTv.setText("");
        }
        mBucketSpecSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getBucketSpecInfo()));

        try {
            MyVars.config.getCodeByBucketType(mBucketTypeTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getBucketTypeInfo().isEmpty())
                mBucketTypeTv.setText(MyVars.config.getBucketTypeInfo().get(0).getSpecify());
            else
                mBucketTypeTv.setText("");
        }
        mBucketTypeSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getBucketTypeInfo()));

        try {
            MyVars.config.getCodeByWaterBrand(mWaterBrandTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getWaterBrandInfo().isEmpty())
                mWaterBrandTv.setText(MyVars.config.getWaterBrandInfo().get(0).getSpecify());
            else
                mWaterBrandTv.setText("");
        }
        mWaterBrandSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getWaterBrandInfo()));

        try {
            MyVars.config.getCodeByWaterSpec(mWaterSpecTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getWaterSpecInfo().isEmpty())
                mWaterSpecTv.setText(MyVars.config.getWaterSpecInfo().get(0).getSpecify());
            else
                mWaterSpecTv.setText("");
        }
        mWaterSpecSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getWaterSpecInfo()));

        try {
            MyVars.config.getCodeByBucketProducer(mBucketProducerTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getBucketProducerInfo().isEmpty())
                mBucketProducerTv.setText(MyVars.config.getBucketProducerInfo().get(0).getSpecify());
            else
                mBucketProducerTv.setText("");
        }
        mBucketProducerSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getBucketProducerInfo()));

        try {
            MyVars.config.getCodeByBucketOwner(mBucketOwnerTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getBucketOwnerInfo().isEmpty())
                mBucketOwnerTv.setText(MyVars.config.getBucketOwnerInfo().get(0).getSpecify());
            else
                mBucketOwnerTv.setText("");
        }
        mBucketOwnerSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getBucketOwnerInfo()));

        try {
            MyVars.config.getCodeByBucketUser(mBucketUserTv.getText().toString());
        } catch (IllegalArgumentException e) {
            if (!MyVars.config.getBucketUserInfo().isEmpty())
                mBucketUserTv.setText(MyVars.config.getBucketUserInfo().get(0).getSpecify());
            else
                mBucketUserTv.setText("");
        }
        mBucketUserSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_specify, MyVars.config.getBucketUserInfo()));
    }

    private void writeTaskSuccess() {
        mProducts.add(0, mProductToRegister);
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
        return MyVars.getReader().isConnected() & !mIsRegistering && !mIsEditing &&
                mIsAllConnectionsReady && mIsUnregisteredEPCRead && mIsBodyCodeWritten;
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R0Fragment> mOuter;

        InnerHandler(R0Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R0Fragment outer = mOuter.get();
            boolean isEditing = outer.mIsEditing;
            switch (msg.what) {
                case MSG_SWITCH_CARD_VISIBILITY:
                    outer.mModifyBtn.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mCancelBtn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mSaveBtn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mBucketSpecTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mBucketSpecSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mBucketTypeTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mBucketTypeSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mWaterBrandTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mWaterBrandSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mWaterSpecTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mWaterSpecSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mBucketProducerTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mBucketProducerSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mBucketOwnerTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mBucketOwnerSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    outer.mBucketUserTv.setVisibility(isEditing ? View.GONE : View.VISIBLE);
                    outer.mBucketUserSpn.setVisibility(isEditing ? View.VISIBLE : View.GONE);
                    break;
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
                        MyParams.EPCType epcType = CommonUtils.validEPC(epc);
                        switch (epcType) {
                            case BUCKET: // 检测到注册桶标签，也允许注册
                            case NONE: // 检测到未注册桶标签，允许注册
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
                        outer.mEpcTv.setBackgroundColor(outer.getResources().getColor(R.color.white));
                        //outer.mEpcTv.setText(CommonUtils.bytesToHex(epc));
                        outer.mEpcTv.setText(CommonUtils.bytesToHex(epc) + epcType.getComment());
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
                    if (outer.mAutoCompleteSbtn.isChecked()) {
                        outer.mBodyCodeIcl.setCode(String.format("%06d", ++outer.mBodyCode));
                        ConfigHelper.setParam(MyParams.S_CODE, String.valueOf(outer.mBodyCode));
                    } else {
                        outer.mBodyCodeIcl.clear();
                        outer.mIsBodyCodeWritten = false;
                    }
                    outer.mIsRegistering = false;
                    outer.mRegisteredAdapter.notifyDataSetChanged();
                    outer.mRegisteredCountTv.setText(String.valueOf(outer.mProducts.size()));
                    outer.mModifyBtn.setEnabled(true);
                    outer.mAutoCompleteSbtn.setEnabled(true);
                    outer.mRegisterBtn.setEnabled(false);
                    outer.playSound(0, 1);
                    break;
                case MSG_FAILED:
                    outer.mIsRegistering = false;
                    outer.mModifyBtn.setEnabled(true);
                    outer.mAutoCompleteSbtn.setEnabled(true);
                    outer.mRegisterBtn.setEnabled(outer.canRegister());
                    break;
            }
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
                    mProductToRegister.setTid(mDataRead);
                }

                // 通过向平台发送TID与桶身码的参数，根据返回结果判定是否重复注册
                writeHint("检查重复注册");
                MessageQuery messageQuery = new MessageQuery(CommonUtils.bytesToHex(mProductToRegister.getTid()),
                        mProductToRegister.getBodyCode());
                Response<Reply> queryResponse = NetHelper.getInstance().checkBodyCodeAndTID(messageQuery).execute();
                if (!queryResponse.isSuccessful()) {
                    writeHint("平台连接失败");
                    writeTaskFailed();
                    return;
                }
                else if (queryResponse.body() != null && queryResponse.body().getCode() == 210) {
                    writeHint("TID已注册");
                    writeTaskFailed();
                    return;
                }
                else if (queryResponse.body() != null && queryResponse.body().getCode() == 211) {
                    if (mAutoCompleteSbtn.isChecked()) {
                        mBodyCodeIcl.setCode(String.format("%06d", ++mBodyCode));
                        ConfigHelper.setParam(MyParams.S_CODE, String.valueOf(mBodyCode));
                    }
                    writeHint("桶身码已注册");
                    writeTaskFailed();
                    return;
                }

                // 写入PC
                writeHint("写入PC");
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        1, CommonUtils.hexToBytes(MyParams.BUCKET_PC_CONTENT)), WRITE_MAX_TRY_COUNT);
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"),
                        InsHelper.MemBankType.EPC,
                        1, 1), READ_MAX_TRY_COUNT);
                Thread.sleep((WRITE_MAX_TRY_COUNT + READ_MAX_TRY_COUNT + 1) * LinkType.getSendInterval());
                if (mDataRead == null || ((mDataRead[0] & 0xFF) >> 3) != mProductToRegister.getEpc().length / 2) {
                    writeHint("写入PC失败");
                    writeTaskFailed();
                    return;
                }

                // 写入EPC
                writeHint("写入EPC");
                mDataRead = null;
                MyVars.getReader().sendCommand(InsHelper.getWriteMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, mProductToRegister.getEpc()), WRITE_MAX_TRY_COUNT);
                MyVars.getReader().sendCommand(
                        InsHelper.getEPCSelectParameter(mProductToRegister.getEpc()), 2);
                MyVars.getReader().sendCommand(InsHelper.getReadMemBank(
                        CommonUtils.hexToBytes("00000000"), InsHelper.MemBankType.EPC,
                        2, MyParams.EPC_BUCKET_LENGTH / 2), READ_MAX_TRY_COUNT);
                Thread.sleep((WRITE_MAX_TRY_COUNT + READ_MAX_TRY_COUNT + 3) * LinkType.getSendInterval());
                if (!Arrays.equals(mDataRead, mProductToRegister.getEpc())) {
                    writeHint("写入EPC失败");
                    writeTaskFailed();
                    return;
                }

                // 尝试上报平台
                writeHint("上报平台");
                MessageRegister message = new MessageRegister();
                message.setBucketspec(mBucketSpecTv.getText().toString());
                message.setBuckettype(mBucketTypeTv.getText().toString());
                message.setWaterbrand(mWaterBrandTv.getText().toString());
                message.setWaterspec(mWaterSpecTv.getText().toString());
                message.setBucketproducer(mBucketProducerTv.getText().toString());
                message.setBucketowner(mBucketOwnerTv.getText().toString());
                message.setBucketuser(mBucketUserTv.getText().toString());
                message.addBucketInfo(
                        CommonUtils.bytesToHex(mProductToRegister.getTid()),
                        CommonUtils.bytesToHex(mProductToRegister.getEpc()),
                        mProductToRegister.getBodyCode());
                Response<Reply> responseR0 = NetHelper.getInstance().uploadR0Message(message).execute();
                if (!responseR0.isSuccessful()) {
                    writeHint("平台连接失败");
                    writeTaskFailed();
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                writeHint("网络通信失败");
                writeTaskFailed();
                return;
            }
            writeHint(mProductToRegister.getBodyCode() + " 注册成功");
            writeTaskSuccess();
        }
    }
}
