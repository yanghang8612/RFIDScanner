package com.casc.rfidscanner.fragment;

import android.app.Activity;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.IntegerRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.BaseActivity;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageAdminLogin;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BatteryStatusMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 各阶段Fragment的基类
 */
public abstract class BaseFragment extends Fragment implements InsHandler {

    private static final String TAG = BaseFragment.class.getSimpleName();

    protected static final int MSG_RECEIVED_FRAME_FROM_READER = 0xBB;

    @BindView(R.id.ll_connection_status) LinearLayout mConnectionStatusLl;
    @BindView(R.id.ll_monitor_status) LinearLayout mMonitorStatusLl;
    @BindView(R.id.ll_reader_status) LinearLayout mReaderStatusLl;
    @BindView(R.id.iv_monitor_status) ImageView mMonitorStatusIv;
    @BindView(R.id.iv_reader_status) ImageView mReaderStatusIv;
    @BindView(R.id.iv_network_status ) ImageView mNetworkStatusIv;
    @BindView(R.id.iv_platform_status) ImageView mPlatformStatusIv;
    @BindView(R.id.iv_battery_status) ImageView mBatteryStatusIv;
    @BindView(R.id.tv_time_hour) TextView mTimeHourIv;
    @BindView(R.id.tv_time_colon) TextView mTimeColonIv;
    @BindView(R.id.tv_time_minute) TextView mTimeMinuteIv;
    @BindView(R.id.btn_backdoor) Button mBackdoorBtn;

    // Fragment所属Activity上下文
    protected Context mContext;

    // 运维专用卡扫描计数器
    protected int mAdminCardScannedCount;

    // 播放实例
    private SoundPool mSoundPool =
            new SoundPool.Builder()
                    .setMaxStreams(10)
                    .setAudioAttributes(new AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build())
                    .build();

    // 资源ID
    private int mSoundID;

    private int mBackdoorCount;

    private Toast mToast;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BatteryStatusMessage message) {
        if (mBatteryStatusIv == null)
            return;
        if (message.batteryPct < 0.2) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_20 :
                    R.drawable.ic_battery_alert);
        } else if (message.batteryPct < 0.3) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_20 :
                    R.drawable.ic_battery_20);
        } else if (message.batteryPct < 0.5) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_30 :
                    R.drawable.ic_battery_30);
        } else if (message.batteryPct < 0.6) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_50 :
                    R.drawable.ic_battery_50);
        } else if (message.batteryPct < 0.8) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_60 :
                    R.drawable.ic_battery_60);
        } else if (message.batteryPct < 0.9) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_80 :
                    R.drawable.ic_battery_80);
        } else if (message.batteryPct < 0.95) {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_90 :
                    R.drawable.ic_battery_90);
        } else {
            mBatteryStatusIv.setImageResource(message.isCharging ?
                    R.drawable.ic_battery_charging_full :
                    R.drawable.ic_battery_full);
        }
    }

    @CallSuper
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        mMonitorStatusIv.setImageResource(MyVars.server.isOnline() ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mReaderStatusIv.setImageResource(message.readerStatus ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mNetworkStatusIv.setImageResource(message.networkStatus ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mPlatformStatusIv.setImageResource(message.platformStatus ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mTimeHourIv.setText(
                new SimpleDateFormat("HH", Locale.CHINA).format(new Date(System.currentTimeMillis())));
        mTimeMinuteIv.setText(
                new SimpleDateFormat("mm", Locale.CHINA).format(new Date(System.currentTimeMillis())));
        mTimeColonIv.setVisibility(
                mTimeColonIv.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        mSoundID = mSoundPool.load(getContext(), R.raw.timer, 1);
        mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        EventBus.getDefault().register(this);
        MyVars.usbReader.setHandler(this);
        MyVars.bleReader.setHandler(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (MyVars.batteryStatus != null) onMessageEvent(MyVars.batteryStatus);
        MyVars.fragmentExecutor = Executors.newScheduledThreadPool(5);
        if (MyParams.ENABLE_BACKDOOR) {
            MyVars.fragmentExecutor.scheduleWithFixedDelay(new BackdoorTask(), 0, 10, TimeUnit.MILLISECONDS);
        }
        initFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (LinkType.getType().isNeedReader)
            MyVars.getReader().start();
        else
            MyVars.getReader().stop();
    }

    @Override
    public void onPause() {
        super.onPause();
        MyVars.getReader().pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mToast.cancel();
        EventBus.getDefault().unregister(this);
        MyVars.fragmentExecutor.shutdown();
        MyVars.cache.clear();
    }

    // 派生类必须重写该abstract方法，以实现自己的Fragment初始化逻辑
    protected abstract void initFragment();

    // 派生类所加载的LayoutID
    protected abstract int getLayout();

    protected void playSound() {
        mSoundPool.play(mSoundID, 1, 1, 10, 0, 1.0F);
    }

    protected void increaseCount(TextView view) {
        int count = Integer.valueOf(view.getText().toString());
        view.setText(String.valueOf(++count));
    }

    protected void decreaseCount(TextView view) {
        int count = Integer.valueOf(view.getText().toString());
        view.setText(String.valueOf(--count));
    }

    protected void sendAdminLoginMessage(String epc) {
        final MessageAdminLogin login = new MessageAdminLogin(epc);
        NetHelper.getInstance()
                .uploadAdminLoginInfo(CommonUtils.generateRequestBody(login))
                .enqueue(new Callback<Reply>() {
                    @Override
                    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                        Reply body = response.body();
                        if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                            MyVars.cache.storeLoginInfo(new Gson().toJson(login));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                        MyVars.cache.storeLoginInfo(new Gson().toJson(login));
                    }
                });
    }

    protected void showToast(String content) {
        mToast.setText(content);
        mToast.show();
    }

    protected void showToast(@StringRes int contentRes) {
        mToast.setText(contentRes);
        mToast.show();
    }

    private class BackdoorTask implements Runnable {

        @Override
        public void run() {
            if (mBackdoorBtn.isPressed()) {
                mBackdoorCount++;
            } else {
                mBackdoorCount = 0;
            }
            if (mBackdoorCount == 10) {
                MyVars.getReader().pause();
                ConfigActivity.actionStart(getContext());
            }
        }
    }
}
