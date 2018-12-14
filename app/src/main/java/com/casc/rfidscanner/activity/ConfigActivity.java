package com.casc.rfidscanner.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.ReaderAdapter;
import com.casc.rfidscanner.bean.Config;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.utils.ClsUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.google.gson.Gson;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.RegexpValidator;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ConfigActivity extends BaseActivity {

    private static final String TAG = ConfigActivity.class.getSimpleName();

    private static final int DISCOVERY_INTERVAL = 15; // s

    public static void actionStart(Context context) {
        if (!(ActivityCollector.getTopActivity() instanceof ConfigActivity)) {
            MyVars.getReader().pause();
            Intent intent = new Intent(context, ConfigActivity.class);
            context.startActivity(intent);
        }
    }

    @BindView(R.id.toolbar_config) Toolbar mToolbar;
    @BindView(R.id.spn_config_link) BetterSpinner mLinkSpn;
    @BindView(R.id.sw_config_usb_switch) Switch mUSBSw;
    @BindView(R.id.sw_config_sensor_switch) Switch mSensorSw;
    @BindView(R.id.spn_config_rssi_threshold) BetterSpinner mRSSIThreshold;
    @BindView(R.id.spn_config_min_reach_times) BetterSpinner mMinReachTimesSpn;
    @BindView(R.id.spn_config_reader_power) BetterSpinner mReaderPowerSpn;
    @BindView(R.id.spn_config_reader_q_value) BetterSpinner mReaderQValueSpn;
    @BindView(R.id.spn_config_tag_lifecycle) BetterSpinner mTagLifecycleSpn;
    @BindView(R.id.spn_config_blank_interval) BetterSpinner mBlankIntervalSpn;
    @BindView(R.id.spn_config_discovery_interval) BetterSpinner mDiscoveryIntervalSpn;

    @BindView(R.id.met_config_longitude) MaterialEditText mLongitudeMet;
    @BindView(R.id.met_config_latitude) MaterialEditText mLatitudeMet;
    @BindView(R.id.met_config_height) MaterialEditText mHeightMet;

    @BindView(R.id.ll_config_line_name) LinearLayout mLineNameLl;
    @BindView(R.id.met_config_line_name) MaterialEditText mLineNameMet;
    @BindView(R.id.met_config_reader_id) MaterialEditText mReaderIDMet;
    @BindView(R.id.met_config_main_platform_addr) MaterialEditText mMainPlatformAddrMet;
    @BindView(R.id.met_config_monitor_app_addr) MaterialEditText mMonitorAppAddrMet;
    @BindView(R.id.met_config_reader_mac) TextView mReaderMacTv;

    @BindView(R.id.rv_reader_list) RecyclerView mReaderRv;

    private CountDownTimer mDiscoveryTimer;

    private ReaderAdapter mReaderAdapter;

    private BluetoothAdapter mBLEAdapter = BluetoothAdapter.getDefaultAdapter();

    private List<BluetoothDevice> mReaders = new ArrayList<>();

    private int mSelectedIndex;

    private long mClickTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        ButterKnife.bind(this);
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
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

//        BluetoothDevice connectedDevice = ((BLEReaderImpl) MyVars.bleReader).getConnectedDevice();
//        if (connectedDevice != null) mReaders.add(connectedDevice);
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

        mDiscoveryTimer = new DiscoveryTimer(DISCOVERY_INTERVAL * 1000, 1000);
        mDiscoveryTimer.start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyVars.getReader().pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDiscoveryTimer.cancel();
        mBLEAdapter.cancelDiscovery();
        MyVars.getReader().start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        LinkType linkType = LinkType.getType();
        mLinkSpn.setText(linkType.comment);
        mLinkSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.link)));

        mUSBSw.setChecked(ConfigHelper.getBool(MyParams.S_USB_SWITCH));
        mSensorSw.setChecked(ConfigHelper.getBool(MyParams.S_SENSOR_SWITCH));

        mRSSIThreshold.setText(ConfigHelper.getString(MyParams.S_RSSI_THRESHOLD));
        mRSSIThreshold.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.rssi_threshold)));

        mMinReachTimesSpn.setText(ConfigHelper.getString(MyParams.S_MIN_REACH_TIMES));
        mMinReachTimesSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.min_reach_times)));

        mReaderPowerSpn.setText(ConfigHelper.getString(MyParams.S_POWER));
        mReaderPowerSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_power)));

        mReaderQValueSpn.setText(ConfigHelper.getString(MyParams.S_Q_VALUE));
        mReaderQValueSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_q_value)));

        mTagLifecycleSpn.setText(ConfigHelper.getString(MyParams.S_TAG_LIFECYCLE));
        mTagLifecycleSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.tag_lifecycle)));

        mBlankIntervalSpn.setText(ConfigHelper.getString(MyParams.S_BLANK_INTERVAL));
        mBlankIntervalSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.blank_interval)));

        mDiscoveryIntervalSpn.setText(ConfigHelper.getString(MyParams.S_DISCOVERY_INTERVAL));
        mDiscoveryIntervalSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.discovery_interval)));

        mLongitudeMet.setText(ConfigHelper.getString(MyParams.S_LONGITUDE));
        mLongitudeMet.addValidator(new RegexpValidator("范围或格式错误(2位小数)",
                "^-?((0|1?[0-7]?[0-9]?)(([.][0-9]{1,2})?)|180(([.][0]{1,2})?))$"));
        mLatitudeMet.setText(ConfigHelper.getString(MyParams.S_LATITUDE));
        mLatitudeMet.addValidator(new RegexpValidator("范围或格式错误(2位小数)",
                "^-?((0|[1-8]?[0-9]?)(([.][0-9]{2})?)|90(([.][0]{2})?))$"));
        mHeightMet.setText(ConfigHelper.getString(MyParams.S_HEIGHT));
        mHeightMet.addValidator(new RegexpValidator("格式错误(2位小数)",
                "^-?(0|[0-9]+)[.][0-9]{2}$"));

        mReaderIDMet.setText(ConfigHelper.getString(MyParams.S_READER_ID));
        mReaderIDMet.addValidator(new RegexpValidator("24位读写器ID(字符仅含0-9、A-F)",
                "^([0-9A-F]{24})$"));
        mMainPlatformAddrMet.setText(ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR));
        mMainPlatformAddrMet.addValidator(new RegexpValidator("网址格式错误",
                "^((([hH][tT][tT][pP][sS]?|[fF][tT][pP])\\:\\/\\/)?([\\w\\.\\-]+(\\:[\\w\\.\\&%\\$\\-]+)*@)?((([^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)(\\.[^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)*(\\.[a-zA-Z]{2,4}))|((([01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d{1,2}|2[0-4]\\d|25[0-5])))(\\b\\:(6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)\\b)?((\\/[^\\/][\\w\\.\\,\\?\\'\\\\\\/\\+&%\\$#\\=~_\\-@]*)*[^\\.\\,\\?\\\"\\'\\(\\)\\[\\]!;<>{}\\s\\x7F-\\xFF])?)$"));
        mMonitorAppAddrMet.setText(ConfigHelper.getString(MyParams.S_STANDBY_PLATFORM_ADDR));
        mMonitorAppAddrMet.addValidator(new RegexpValidator("网址格式错误",
                "^((([hH][tT][tT][pP][sS]?|[fF][tT][pP])\\:\\/\\/)?([\\w\\.\\-]+(\\:[\\w\\.\\&%\\$\\-]+)*@)?((([^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)(\\.[^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)*(\\.[a-zA-Z]{2,4}))|((([01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d{1,2}|2[0-4]\\d|25[0-5])))(\\b\\:(6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)\\b)?((\\/[^\\/][\\w\\.\\,\\?\\'\\\\\\/\\+&%\\$#\\=~_\\-@]*)*[^\\.\\,\\?\\\"\\'\\(\\)\\[\\]!;<>{}\\s\\x7F-\\xFF])?)$"));
        mReaderMacTv.setText(ConfigHelper.getString(MyParams.S_READER_MAC));
    }

    @OnClick(R.id.btn_config_save)
    public void onSaveButtonClicked() {
        if (mLongitudeMet.validate() && mLatitudeMet.validate() && mHeightMet.validate() &&
                mReaderIDMet.validate() && mMainPlatformAddrMet.validate() && mMonitorAppAddrMet.validate()) {
            LinkType linkType = LinkType.getTypeByComment(mLinkSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_LINK, linkType.link);
            ConfigHelper.setParam(MyParams.S_USB_SWITCH, String.valueOf(mUSBSw.isChecked()));
            ConfigHelper.setParam(MyParams.S_SENSOR_SWITCH, String.valueOf(mSensorSw.isChecked()));
            ConfigHelper.setParam(MyParams.S_RSSI_THRESHOLD, mRSSIThreshold.getText().toString());
            ConfigHelper.setParam(MyParams.S_MIN_REACH_TIMES, mMinReachTimesSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_POWER, mReaderPowerSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_Q_VALUE, mReaderQValueSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_TAG_LIFECYCLE, mTagLifecycleSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_BLANK_INTERVAL, mBlankIntervalSpn.getText().toString());
            ConfigHelper.setParam(MyParams.S_DISCOVERY_INTERVAL, mDiscoveryIntervalSpn.getText().toString());

            ConfigHelper.setParam(MyParams.S_MAIN_PLATFORM_ADDR, mMainPlatformAddrMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_STANDBY_PLATFORM_ADDR, mMonitorAppAddrMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_LONGITUDE, mLongitudeMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_LATITUDE, mLatitudeMet.getText().toString());
            ConfigHelper.setParam(MyParams.S_HEIGHT, mHeightMet.getText().toString());

            if (!ConfigHelper.getString(MyParams.S_READER_ID).equals(mReaderIDMet.getText().toString())) {
                ConfigHelper.setParam(MyParams.S_READER_ID, mReaderIDMet.getText().toString());
                NetHelper.getInstance().getConfig().enqueue(new Callback<Reply>() {
                    @Override
                    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                        Reply reply = response.body();
                        if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                            ConfigHelper.setParam(MyParams.S_API_JSON, reply.getContent().toString());
                            MyVars.config = new Gson().fromJson(reply.getContent().toString(), Config.class);
                            EventBus.getDefault().post(new ConfigUpdatedMessage());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {}
                });
            } else {
                ConfigHelper.setParam(MyParams.S_READER_ID, mReaderIDMet.getText().toString());
            }
            ConfigHelper.setParam(MyParams.S_READER_MAC, mReaderMacTv.getText().toString());
            finish();
        }
    }

    @OnClick(R.id.btn_clear_cache)
    public void onClearCacheButtonClicked() {
        MyVars.cache.clear();
        showToast("清除成功");
    }

    @OnClick(R.id.btn_config_exit)
    public void onExitButtonClicked() {
        if (System.currentTimeMillis() - mClickTime < 2000) {
            ActivityCollector.finishAll();
        } else {
            mClickTime = System.currentTimeMillis();
            showToast("再点击一次退出系统");
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
            mDiscoveryTimer = new DiscoveryTimer(DISCOVERY_INTERVAL * 1000, 1000);
            mDiscoveryTimer.start();
        }
    }
}
