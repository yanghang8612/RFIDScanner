package com.casc.rfidscanner.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.ReaderAdapter;
import com.casc.rfidscanner.backend.impl.BLEReaderImpl;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.utils.ClsUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.RegexpValidator;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ConfigActivity extends BaseActivity {

    private static final String TAG = ConfigActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, ConfigActivity.class);
        context.startActivity(intent);
    }

    @BindView(R.id.spn_config_link) BetterSpinner mLinkSpn;
    @BindView(R.id.spn_config_reader_power) BetterSpinner mReaderPowerSpn;
    @BindView(R.id.spn_config_reader_q_value) BetterSpinner mReaderQValueSpn;
    @BindView(R.id.spn_config_reader_send_interval) BetterSpinner mReaderSendIntervalSpn;
    @BindView(R.id.spn_config_tag_lifecycle) BetterSpinner mTagLifecycleSpn;
    @BindView(R.id.spn_config_blank_interval) BetterSpinner mBlankIntervalSpn;

    @BindView(R.id.met_config_longitude) MaterialEditText mLongitudeMet;
    @BindView(R.id.met_config_latitude) MaterialEditText mLatitudeMet;
    @BindView(R.id.met_config_height) MaterialEditText mHeightMet;

    @BindView(R.id.met_config_reader_id) MaterialEditText mReaderIDMet;
    @BindView(R.id.met_config_main_platform_addr) MaterialEditText mMainPlatformAddrMet;
    @BindView(R.id.met_config_standby_platform_addr) MaterialEditText mStandbyPlatformAddrMet;
    @BindView(R.id.met_config_reader_mac) TextView mReaderMacTv;

    @BindView(R.id.rv_reader_list) RecyclerView mReaderRv;

    private CountDownTimer mDiscoveryTimer;

    private ReaderAdapter mReaderAdapter;

    private BluetoothAdapter mBLEAdapter = BluetoothAdapter.getDefaultAdapter();

    private List<BluetoothDevice> mReaders = new ArrayList<>();

    private int mSelectedIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        ButterKnife.bind(this);
        MyVars.getReader().pause();
        initViews();

        // 注册蓝牙广播监听器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); // 发现设备
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); //设备连接状态改变
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED); //蓝牙设备状态改变
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    if(scanDevice == null || scanDevice.getName() == null) return;
                    for (BluetoothDevice device : mReaders)
                        if (device.getAddress().equals(scanDevice.getAddress())) return;
                    mReaders.add(scanDevice);
                    mReaderAdapter.notifyDataSetChanged();
                }
                else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                    if(scanDevice == null || scanDevice.getName() == null) return;
                    mReaderAdapter.notifyDataSetChanged();
                }
            }
        };
        registerReceiver(mReceiver, filter);

        BluetoothDevice connectedDevice = ((BLEReaderImpl) MyVars.bleReader).getConnectedDevice();
        if (connectedDevice != null) mReaders.add(connectedDevice);
        mReaderAdapter = new ReaderAdapter(mReaders);
        mReaderAdapter.bindToRecyclerView(mReaderRv);
        mReaderAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (mReaders.get(position).getBondState() != BluetoothDevice.BOND_BONDED) return;
                View preView = adapter.getViewByPosition(mSelectedIndex, R.id.ll_reader_content);
                if (preView != null)
                    preView.setBackground(getDrawable(R.drawable.bg_reader_normal));
                if (view != null)
                    view.findViewById(R.id.ll_reader_content).setBackground(getDrawable(R.drawable.bg_reader_selected));
                mSelectedIndex = position;
                mReaderMacTv.setText(mReaders.get(position).getAddress());
            }
        });
        mReaderAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                if (view instanceof Button) {
                    try {
                        ClsUtils.createBond(mReaders.get(position).getClass(), mReaders.get(position));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mReaderRv.setLayoutManager(new LinearLayoutManager(this));
        mReaderRv.setAdapter(mReaderAdapter);

        mLinkSpn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                LinkType linkType = LinkType.getTypeByComment(s.toString());
                mReaderPowerSpn.setText(ConfigHelper.getParam(linkType.power));
                mReaderQValueSpn.setText(ConfigHelper.getParam(linkType.qValue));
                mReaderSendIntervalSpn.setText(ConfigHelper.getParam(linkType.sendInterval));
            }
        });

        mDiscoveryTimer = new DiscoveryTimer(5 * 1000, 1000);
        mDiscoveryTimer.start();
    }

    @Override
    public void finish() {
        super.finish();
        mDiscoveryTimer.cancel();
        mBLEAdapter.cancelDiscovery();
    }

    private void initViews() {
        LinkType linkType = LinkType.getType();
        mLinkSpn.setText(linkType.comment);
        mLinkSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.link)));

        mReaderPowerSpn.setText(ConfigHelper.getParam(linkType.power));
        mReaderPowerSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_power)));

        mReaderQValueSpn.setText(ConfigHelper.getParam(linkType.qValue));
        mReaderQValueSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_q_value)));

        mReaderSendIntervalSpn.setText(ConfigHelper.getParam(linkType.sendInterval));
        mReaderSendIntervalSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_send_interval)));

        mTagLifecycleSpn.setText(ConfigHelper.getParam(MyParams.S_TAG_LIFECYCLE));
        mTagLifecycleSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.tag_lifecycle)));

        mBlankIntervalSpn.setText(ConfigHelper.getParam(MyParams.S_BLANK_INTERVAL));
        mBlankIntervalSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.blank_interval)));

        mLongitudeMet.setText(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        mLongitudeMet.addValidator(new RegexpValidator("范围或格式错误(2位小数)",
                "^-?((0|1?[0-7]?[0-9]?)(([.][0-9]{1,2})?)|180(([.][0]{1,2})?))$"));
        mLatitudeMet.setText(ConfigHelper.getParam(MyParams.S_LATITUDE));
        mLatitudeMet.addValidator(new RegexpValidator("范围或格式错误(2位小数)",
                "^-?((0|[1-8]?[0-9]?)(([.][0-9]{2})?)|90(([.][0]{2})?))$"));
        mHeightMet.setText(ConfigHelper.getParam(MyParams.S_HEIGHT));
        mHeightMet.addValidator(new RegexpValidator("格式错误(2位小数)",
                "^-?(0|[0-9]+)[.][0-9]{2}$"));

        mReaderIDMet.setText(ConfigHelper.getParam(MyParams.S_READER_ID));
        mReaderIDMet.addValidator(new RegexpValidator("24位读写器ID(字符仅含0-9、A-F)",
                "^([0-9A-F]{24})$"));
        mMainPlatformAddrMet.setText(ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR));
        mMainPlatformAddrMet.addValidator(new RegexpValidator("网址格式错误",
                "^((([hH][tT][tT][pP][sS]?|[fF][tT][pP])\\:\\/\\/)?([\\w\\.\\-]+(\\:[\\w\\.\\&%\\$\\-]+)*@)?((([^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)(\\.[^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)*(\\.[a-zA-Z]{2,4}))|((([01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d{1,2}|2[0-4]\\d|25[0-5])))(\\b\\:(6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)\\b)?((\\/[^\\/][\\w\\.\\,\\?\\'\\\\\\/\\+&%\\$#\\=~_\\-@]*)*[^\\.\\,\\?\\\"\\'\\(\\)\\[\\]!;<>{}\\s\\x7F-\\xFF])?)$"));
        mStandbyPlatformAddrMet.setText(ConfigHelper.getParam(MyParams.S_STANDBY_PLATFORM_ADDR));
        mStandbyPlatformAddrMet.addValidator(new RegexpValidator("网址格式错误",
                "^((([hH][tT][tT][pP][sS]?|[fF][tT][pP])\\:\\/\\/)?([\\w\\.\\-]+(\\:[\\w\\.\\&%\\$\\-]+)*@)?((([^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)(\\.[^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)*(\\.[a-zA-Z]{2,4}))|((([01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d{1,2}|2[0-4]\\d|25[0-5])))(\\b\\:(6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)\\b)?((\\/[^\\/][\\w\\.\\,\\?\\'\\\\\\/\\+&%\\$#\\=~_\\-@]*)*[^\\.\\,\\?\\\"\\'\\(\\)\\[\\]!;<>{}\\s\\x7F-\\xFF])?)$"));
        mReaderMacTv.setText(ConfigHelper.getParam(MyParams.S_READER_MAC));
    }

    @OnClick(R.id.btn_config_resume)
    public void onResumeButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_config_save)
    public void onSaveButtonClicked() {
        if (mLongitudeMet.validate() && mLatitudeMet.validate() && mHeightMet.validate() &&
                mReaderIDMet.validate() && mMainPlatformAddrMet.validate() && mStandbyPlatformAddrMet.validate()) {
            LinkType linkType = LinkType.getTypeByComment(mLinkSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_LINK, linkType.link);
            ConfigHelper.setParam(linkType.power, mReaderPowerSpn.getText().toString());
            ConfigHelper.setParam(linkType.qValue, mReaderQValueSpn.getText().toString());
            ConfigHelper.setParam(linkType.sendInterval, mReaderSendIntervalSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_TAG_LIFECYCLE, mTagLifecycleSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_BLANK_INTERVAL, mBlankIntervalSpn.getText().toString());

            ConfigHelper.setParam(MyParams.S_LONGITUDE, mLongitudeMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_LATITUDE, mLatitudeMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_HEIGHT, mHeightMet.getText().toString());

            ConfigHelper.setParam(MyParams.S_READER_ID, mReaderIDMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_MAIN_PLATFORM_ADDR, mMainPlatformAddrMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_STANDBY_PLATFORM_ADDR, mStandbyPlatformAddrMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_READER_MAC, mReaderMacTv.getText().toString());
            finish();
        }
    }

    private class DiscoveryTimer extends CountDownTimer {

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public DiscoveryTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
            mBLEAdapter.startDiscovery();
        }

        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            mBLEAdapter.cancelDiscovery();
            mDiscoveryTimer = new DiscoveryTimer(5 * 1000, 1000);
            mDiscoveryTimer.start();
        }
    }
}
