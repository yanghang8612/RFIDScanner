package com.casc.rfidscanner;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;
import com.casc.rfidscanner.backend.TagCache;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.backend.impl.BLEReaderImpl;
import com.casc.rfidscanner.backend.impl.USBReaderImpl;
import com.casc.rfidscanner.bean.Config;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageConfig;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    private WifiManager wifiManager;

    private ConnectivityManager connectivityManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // 程序崩溃捕捉并打印响应信息
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());

        // 初始化相关字段
        instance = this;
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // 初始化读写器实例（USB、蓝牙）和标签缓存
        MyVars.executor = Executors.newScheduledThreadPool(10);
        MyVars.usbReader = new USBReaderImpl(this);
        MyVars.bleReader = new BLEReaderImpl(this);
        MyVars.cache = new TagCache(this);

        // 初始化Speech相关授权及参数
        SpeechSynthesizer speechSynthesizer = SpeechSynthesizer.getInstance();
        speechSynthesizer.setContext(this);
        speechSynthesizer.setAppId(MyParams.AppId);
        speechSynthesizer.setApiKey(MyParams.AppKey, MyParams.AppSecret);
        speechSynthesizer.auth(TtsMode.MIX);
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI);
        speechSynthesizer.initTts(TtsMode.MIX);

        // 初始化配置，并启动配置信息更新线程
        if (MyVars.config == null) {
            MyVars.config = new Gson().fromJson(ConfigHelper.getParam(MyParams.S_API_JSON), Config.class);
        }
        MyVars.executor.scheduleWithFixedDelay(new UpdateConfigTask(), 0 , MyParams.CONFIG_UPDATE_INTERVAL, TimeUnit.SECONDS);
        MyVars.executor.scheduleWithFixedDelay(new InternetStatusCheckTask(), 0, MyParams.INTERNET_STATUS_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new PlatformStatusCheckTask(), 0, MyParams.PLATFORM_STATUS_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.i(TAG, "App terminated");
        MyVars.executor.shutdown();
    }

    private class UpdateConfigTask implements Runnable {

        @Override
        public void run() {
            if (MyVars.getReader().getState() != TagReader.STATE_CONNECTING) {
                NetHelper.getInstance().getConfig(new MessageConfig()).enqueue(new Callback<Reply>() {
                    @Override
                    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                        Reply reply = response.body();
                        //Log.i(TAG, reply.toString());
                        if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                            ConfigHelper.setParam(MyParams.S_API_JSON, reply.getContent().toString());
                            MyVars.config = new Gson().fromJson(reply.getContent().toString(), Config.class);
                            EventBus.getDefault().post(new ConfigUpdatedMessage());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {}
                });
            }
        }
    }

    private class InternetStatusCheckTask implements Runnable {

        @Override
        public void run() {
            if (wifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED)
                wifiManager.setWifiEnabled(true);
            NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            EventBus.getDefault().post(MyVars.status.setReaderStatus(MyVars.getReader().isConnected())
                    .setNetworkStatus(networkCapabilities != null &&
                            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)));
        }
    }

    private class PlatformStatusCheckTask implements Runnable {

        @Override
        public void run() {
            if (MyVars.getReader().getState() != TagReader.STATE_CONNECTING) {
                NetHelper.getInstance().sendHeartbeat().enqueue(new Callback<Reply>() {
                    @Override
                    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                        if (response.isSuccessful()) {
                            if (!MyVars.status.platformStatus)
                                EventBus.getDefault().post(MyVars.status.setPlatformStatus(true));
                        } else {
                            if (MyVars.status.platformStatus)
                                EventBus.getDefault().post(MyVars.status.setPlatformStatus(false));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                        if (MyVars.status.platformStatus) {
                            EventBus.getDefault().post(MyVars.status.setPlatformStatus(false));
                        }
                    }
                });
            }
        }
    }
}

