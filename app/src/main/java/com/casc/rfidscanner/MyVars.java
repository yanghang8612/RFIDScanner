package com.casc.rfidscanner;

import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.bean.ApiConfig;
import com.casc.rfidscanner.bean.Stack;
import com.casc.rfidscanner.dao.DataCache;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.bean.Config;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.activity.ActivityCollector;
import com.google.gson.Gson;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class MyVars {

    private MyVars(){}

    private static TagReader preReader = null;

    public static TagReader usbReader = null;

    public static TagReader bleReader = null;

    public static Stack stackToShow = null;

    public static ApiConfig api = new Gson().fromJson(
            SpHelper.getString(MyParams.S_API_JSON), ApiConfig.class);

    public static Config config = new Gson().fromJson(
            SpHelper.getString(MyParams.S_CONFIG_JSON), Config.class);

    public static ScheduledExecutorService executor = Executors.newScheduledThreadPool(15);

    public static MultiStatusMessage status = new MultiStatusMessage();

    public static DataCache cache = new DataCache();

    public static TagReader getReader() {
        if (preReader != null && ActivityCollector.getTopActivity() instanceof ConfigActivity) {
            return preReader;
        }
        if (usbReader.isConnected()) {
            if (preReader != usbReader) {
                preReader = usbReader;
                usbReader.start();
                bleReader.stop();
            }
            return usbReader;
        } else {
            if (preReader != bleReader) {
                preReader = bleReader;
                usbReader.stop();
                bleReader.start();
            }
            return bleReader;
        }
    }
}
