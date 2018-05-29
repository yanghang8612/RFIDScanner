package com.casc.rfidscanner.helper;

import android.content.Context;
import android.content.SharedPreferences;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyParams;

/**
 * 配置信息存储辅助类
 */
public class ConfigHelper {

    private static final String TAG = ConfigHelper.class.getSimpleName();
    private static final String FILE_NAME = "RFIDScannerSP";
    private static final int CONTEXT_MODEL = Context.MODE_PRIVATE;
    private static final SharedPreferences SP =
            MyApplication.getInstance().getSharedPreferences(FILE_NAME, CONTEXT_MODEL);

    public static String getParam(String key) {
        if (!MyParams.CONFIG_DEFAULT_MAP.containsKey(key))
            throw new IllegalArgumentException("No matched key");
        String defaultValue = MyParams.CONFIG_DEFAULT_MAP.get(key);
        return SP.getString(key, defaultValue);
    }

    public static int getIntegerParam(String key) {
        return Integer.valueOf(getParam(key).replaceAll("[a-zA-Z]", ""));
    }

    public static boolean getBooleanParam(String key) {
        return Boolean.valueOf(getParam(key));
    }

    /**
     * 保存配置信息
     *
     * @param key 要set的preference的key值
     * @param value 要set的preference的value值
     */
    public static void setParam(String key, String value) {
        SharedPreferences.Editor editor = SP.edit();
        editor.putString(key, value);
        editor.apply();
    }

    /**
     * 清除所有数据
     */
    public static void clearAll() {
        SharedPreferences.Editor editor = SP.edit();
        editor.clear().apply();
    }

    /**
     * 清除指定数据
     */
    public static void clear(Context context, String key) {
        SharedPreferences.Editor editor = SP.edit();
        editor.remove(key);
        editor.apply();
    }
}

