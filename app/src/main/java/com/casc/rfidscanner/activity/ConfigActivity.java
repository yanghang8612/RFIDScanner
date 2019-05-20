package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Switch;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.ApiConfig;
import com.casc.rfidscanner.bean.Config;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.EmptyAdapter;
import com.casc.rfidscanner.helper.net.SuccessAdapter;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.ParamsChangedMessage;
import com.google.gson.Gson;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.rengwuxian.materialedittext.validation.RegexpValidator;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;

import butterknife.BindView;
import butterknife.OnClick;

public class ConfigActivity extends BaseActivity {

    private static final String TAG = ConfigActivity.class.getSimpleName();

    public static void actionStart(Context context, String adminCard) {
        if (ActivityCollector.topNotOf(ConfigActivity.class)) {
            if (TextUtils.isEmpty(adminCard)) {
                NetHelper.getInstance().uploadAdminLoginMsg(adminCard).enqueue(new EmptyAdapter());
            }
            Intent intent = new Intent(context, ConfigActivity.class);
            context.startActivity(intent);
        }
    }

    private long mClickTime;

    @BindView(R.id.toolbar_config) Toolbar mToolbar;
    @BindView(R.id.spn_config_link) BetterSpinner mLinkSpn;
    @BindView(R.id.sw_config_sensor_switch) Switch mSensorSw;
    @BindView(R.id.spn_config_rssi_threshold) BetterSpinner mRSSIThreshold;
    @BindView(R.id.spn_config_min_reach_times) BetterSpinner mMinReachTimesSpn;
    @BindView(R.id.spn_config_reader_power) BetterSpinner mReaderPowerSpn;
    @BindView(R.id.spn_config_reader_q_value) BetterSpinner mReaderQValueSpn;
    @BindView(R.id.spn_config_tag_lifecycle) BetterSpinner mTagLifecycleSpn;

    @BindView(R.id.met_config_reader_id) MaterialEditText mReaderIDMet;
    @BindView(R.id.met_config_device_addr) MaterialEditText mDeviceAddrMet;
    @BindView(R.id.met_config_longitude) MaterialEditText mLongitudeMet;
    @BindView(R.id.met_config_latitude) MaterialEditText mLatitudeMet;
    @BindView(R.id.met_config_height) MaterialEditText mHeightMet;

    @OnClick(R.id.btn_config_save) void onSaveButtonClicked() {
        if (mLongitudeMet.validate() && mLatitudeMet.validate() && mHeightMet.validate() &&
                mReaderIDMet.validate() && mDeviceAddrMet.validate()) {
            LinkType linkType = LinkType.getTypeByComment(mLinkSpn.getText().toString());
            SpHelper.setParam(MyParams.S_LINK, linkType.link);
            SpHelper.setParam(MyParams.S_SENSOR_SWITCH, String.valueOf(mSensorSw.isChecked()));
            SpHelper.setParam(MyParams.S_RSSI_THRESHOLD, mRSSIThreshold.getText().toString());
            SpHelper.setParam(MyParams.S_MIN_REACH_TIMES, mMinReachTimesSpn.getText().toString());
            SpHelper.setParam(MyParams.S_POWER, mReaderPowerSpn.getText().toString());
            SpHelper.setParam(MyParams.S_Q_VALUE, mReaderQValueSpn.getText().toString());
            SpHelper.setParam(MyParams.S_TAG_LIFECYCLE, mTagLifecycleSpn.getText().toString());

            SpHelper.setParam(MyParams.S_READER_ID, mReaderIDMet.getText().toString());
            SpHelper.setParam(MyParams.S_DEVICE_ADDR, mDeviceAddrMet.getText().toString());
            SpHelper.setParam(MyParams.S_LONGITUDE, mLongitudeMet.getText().toString());
            SpHelper.setParam(MyParams.S_LATITUDE, mLatitudeMet.getText().toString());
            SpHelper.setParam(MyParams.S_HEIGHT, mHeightMet.getText().toString());

            EventBus.getDefault().post(new ParamsChangedMessage());
            finish();
        }
    }

    @OnClick(R.id.btn_clear_cache) void onClearCacheButtonClicked() {
        MyVars.cache.clear();
        showToast("清除成功");
    }

    @OnClick(R.id.btn_config_exit) void onExitButtonClicked() {
        if (System.currentTimeMillis() - mClickTime < 2000) {
            ActivityCollector.finishAll();
        } else {
            mClickTime = System.currentTimeMillis();
            showToast("再点击一次退出系统");
        }
    }

    @Override
    protected void initActivity() {
        setSupportActionBar(mToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        initViews();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_config;
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyVars.getReader().pause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyVars.getReader().start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        LinkType linkType = LinkType.getType();
        mLinkSpn.setText(linkType.comment);
        mLinkSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.link)));

        mRSSIThreshold.setText(SpHelper.getString(MyParams.S_RSSI_THRESHOLD));
        mRSSIThreshold.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.rssi_threshold)));

        mMinReachTimesSpn.setText(SpHelper.getString(MyParams.S_MIN_REACH_TIMES));
        mMinReachTimesSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.min_reach_times)));

        mReaderPowerSpn.setText(SpHelper.getString(MyParams.S_POWER));
        mReaderPowerSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_power)));

        mReaderQValueSpn.setText(SpHelper.getString(MyParams.S_Q_VALUE));
        mReaderQValueSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.reader_q_value)));

        mTagLifecycleSpn.setText(SpHelper.getString(MyParams.S_TAG_LIFECYCLE));
        mTagLifecycleSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_config, getResources().getStringArray(R.array.tag_lifecycle)));

        mLongitudeMet.setText(SpHelper.getString(MyParams.S_LONGITUDE));
        mLongitudeMet.addValidator(new RegexpValidator("范围或格式错误(2位小数)",
                "^-?((0|1?[0-7]?[0-9]?)(([.][0-9]{1,2})?)|180(([.][0]{1,2})?))$"));
        mLatitudeMet.setText(SpHelper.getString(MyParams.S_LATITUDE));
        mLatitudeMet.addValidator(new RegexpValidator("范围或格式错误(2位小数)",
                "^-?((0|[1-8]?[0-9]?)(([.][0-9]{2})?)|90(([.][0]{2})?))$"));
        mHeightMet.setText(SpHelper.getString(MyParams.S_HEIGHT));
        mHeightMet.addValidator(new RegexpValidator("格式错误(2位小数)",
                "^-?(0|[0-9]+)[.][0-9]{2}$"));

        mReaderIDMet.setText(SpHelper.getString(MyParams.S_READER_ID));
        mReaderIDMet.addValidator(new RegexpValidator("10位读写器ID(字符仅含0-9、A-F)",
                "^([0-9A-F]{10})$"));
        mDeviceAddrMet.setText(SpHelper.getString(MyParams.S_DEVICE_ADDR));
        mDeviceAddrMet.addValidator(new RegexpValidator("网址格式错误",
                "^((([hH][tT][tT][pP][sS]?|[fF][tT][pP])\\:\\/\\/)?([\\w\\.\\-]+(\\:[\\w\\.\\&%\\$\\-]+)*@)?((([^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)(\\.[^\\s\\(\\)\\<\\>\\\\\\\"\\.\\[\\]\\,@;:]+)*(\\.[a-zA-Z]{2,4}))|((([01]?\\d{1,2}|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d{1,2}|2[0-4]\\d|25[0-5])))(\\b\\:(6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)\\b)?((\\/[^\\/][\\w\\.\\,\\?\\'\\\\\\/\\+&%\\$#\\=~_\\-@]*)*[^\\.\\,\\?\\\"\\'\\(\\)\\[\\]!;<>{}\\s\\x7F-\\xFF])?)$"));
    }

}
