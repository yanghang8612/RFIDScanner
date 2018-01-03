package com.casc.rfidscanner.helper;

import android.content.Context;

import com.casc.rfidscanner.GlobalParams;

/**
 * 定位信息的存储与获取，包括经纬高
 */
public class GPSInfoHelper {

    private static final Float DEFAULT_VALUE = 0F;

    private static final Float LONGITUDE_THRESHOLD = 180F;
    private static final Float LATITUDE_THRESHOLD = 90F;

    /**
     * 分别检验经纬高数据合的法性，并存储
     *
     * @param context
     * @param longitude
     * @param latitude
     * @param height
     */
    public static void setGPSInfo(Context context, Float longitude, Float latitude, Float height) {
        if (context == null) return;
        if (longitude != null && !Float.isNaN(longitude) && !Float.isInfinite(longitude) && LONGITUDE_THRESHOLD.compareTo(Math.abs(longitude)) != -1)
            SharedPreferencesHelper.setParam(context, GlobalParams.S_LONGITUDE, longitude);
        if (latitude != null && !Float.isNaN(latitude) && !Float.isInfinite(latitude) && LATITUDE_THRESHOLD.compareTo(Math.abs(latitude)) != -1)
            SharedPreferencesHelper.setParam(context, GlobalParams.S_LATITUDE, latitude);
        if (height != null && !Float.isNaN(height) && !Float.isInfinite(height))
            SharedPreferencesHelper.setParam(context, GlobalParams.S_HEIGHT, height);
    }

    /**
     * 获取经度
     *
     * @param context
     * @return
     */
    public static Float getLongitude(Context context) {
        return (Float) SharedPreferencesHelper.getParam(context, GlobalParams.S_LONGITUDE, DEFAULT_VALUE);
    }

    /**
     * 获取纬度
     *
     * @param context
     * @return
     */
    public static Float getLatitude(Context context) {
        return (Float) SharedPreferencesHelper.getParam(context, GlobalParams.S_LATITUDE, DEFAULT_VALUE);
    }

    /**
     * 获取高度
     *
     * @param context
     * @return
     */
    public static Float getHeight(Context context) {
        return (Float) SharedPreferencesHelper.getParam(context, GlobalParams.S_HEIGHT, DEFAULT_VALUE);
    }
}
