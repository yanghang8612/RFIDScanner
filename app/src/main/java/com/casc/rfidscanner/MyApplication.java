package com.casc.rfidscanner;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.util.Log;

import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.TtsMode;
import com.casc.rfidscanner.bean.ApiConfig;
import com.casc.rfidscanner.dao.DataCache;
import com.casc.rfidscanner.backend.impl.BLEReaderImpl;
import com.casc.rfidscanner.backend.impl.USBReaderImpl;
import com.casc.rfidscanner.bean.Config;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.SuccessAdapter;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.ParamsChangedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.sql.NClob;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    private static MyApplication mInstance;

    public static MyApplication getInstance() {
        return mInstance;
    }

    private WifiManager mWifiManager;

    private ConnectivityManager mConnectivityManager;

    @Subscribe
    public void onMessageEvent(ParamsChangedMessage msg) {
        MyVars.executor.execute(new UpdateConfigTask());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        // 程序崩溃捕捉并打印响应信息
        CrashHandler.getInstance().init(this);

        // 初始化相关字段
        mInstance = this;
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // 初始化读写器实例(USB、蓝牙)
        MyVars.usbReader = new USBReaderImpl(this);
        MyVars.bleReader = new BLEReaderImpl(this);

        // 初始化Speech相关授权及参数
        SpeechSynthesizer speechSynthesizer = SpeechSynthesizer.getInstance();
        speechSynthesizer.setContext(this);
        speechSynthesizer.setAppId(MyParams.AppId);
        speechSynthesizer.setApiKey(MyParams.AppKey, MyParams.AppSecret);
        speechSynthesizer.auth(TtsMode.MIX);
        speechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE,
                SpeechSynthesizer.MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI);
        speechSynthesizer.initTts(TtsMode.MIX);

        MyVars.executor.scheduleWithFixedDelay(new UpdateConfigTask(), 2000 , MyParams.CONFIG_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new InternetStatusCheckTask(), 2000, MyParams.INTERNET_STATUS_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new PlatformStatusCheckTask(), 2000, MyParams.PLATFORM_STATUS_CHECK_INTERVAL, TimeUnit.MILLISECONDS);

//        // 上报系统以及软件启动时间
//        String content = "平板启动于（" + CommonUtils.convertDateTime(System.currentTimeMillis() - SystemClock.elapsedRealtime())
//                + "），软件启动于（" + CommonUtils.convertDateTime(System.currentTimeMillis()) + "）";
//        MyVars.cache.storeLogMessage(content);
    }

    private class UpdateConfigTask implements Runnable {

        @Override
        public void run() {
            try {
                Reply apiReply = NetHelper.getInstance().getApiConfig().execute().body();
                if (apiReply != null && apiReply.getCode() == 200) {
                    SpHelper.setParam(MyParams.S_API_JSON, apiReply.getContent().toString());
                    MyVars.api = new Gson().fromJson(apiReply.getContent().toString(), ApiConfig.class);

                    Reply configReply = NetHelper.getInstance().getConfig().execute().body();
                    if (configReply != null && configReply.getCode() == 200) {
                        String content = configReply.getContent().toString();
                        if (!SpHelper.getString(MyParams.S_CONFIG_JSON).equals(content)) {
                            SpHelper.setParam(MyParams.S_CONFIG_JSON, content);
                            MyVars.config = new Gson().fromJson(content, Config.class);
                            EventBus.getDefault().post(new ConfigUpdatedMessage());
                        }
                    } else if (configReply != null) {
                        Log.i(TAG, configReply.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class InternetStatusCheckTask implements Runnable {

        @Override
        public void run() {
            if (mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED)
                mWifiManager.setWifiEnabled(true);
//            NetworkCapabilities nc = mConnectivityManager.getNetworkCapabilities(
//                    mConnectivityManager.getActiveNetwork());
//            MyVars.status.setReaderStatus(MyVars.getReader().isConnected());
//            MyVars.status.setNetworkStatus(nc != null &&
//                    nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED));
            MyVars.status.setNetworkStatus(true);
            if (!MyVars.status.networkStatus) {
                MyVars.status.setPlatformStatus(false);
            }
            EventBus.getDefault().post(MyVars.status);
        }
    }

    private class PlatformStatusCheckTask implements Runnable {

        @Override
        public void run() {
            try {
                Reply reply = NetHelper.getInstance().sendHeartbeat().execute().body();
                MyVars.status.setPlatformStatus(reply != null && reply.getCode() == 200);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

