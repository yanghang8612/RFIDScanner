package com.casc.rfidscanner.helper;

import android.content.Context;

import com.casc.rfidscanner.MyParams;

/**
 * 定位信息的存储与获取，包括经纬高
 */
public class GPSHelper {

    private static final Double LONGITUDE_THRESHOLD = 180.0;
    private static final Double LATITUDE_THRESHOLD = 90.0;

    /**
     * 分别检验经纬高数据合的法性，并存储
     *
     * @param longitude 需要设定的经度数值
     * @param latitude 需要设定的纬度数值
     * @param height 需要设定的高度数值
     */
    public static void setGPSInfo(double longitude, double latitude, double height) {
        if (!Double.isNaN(longitude) && !Double.isInfinite(longitude) && LONGITUDE_THRESHOLD.compareTo(Math.abs(longitude)) != -1)
            SpHelper.setParam(MyParams.S_LONGITUDE, String.valueOf(longitude));
        if (!Double.isNaN(latitude) && !Double.isInfinite(latitude) && LATITUDE_THRESHOLD.compareTo(Math.abs(latitude)) != -1)
            SpHelper.setParam(MyParams.S_LATITUDE, String.valueOf(latitude));
        if (!Double.isNaN(height) && !Double.isInfinite(height))
            SpHelper.setParam(MyParams.S_HEIGHT, String.valueOf(height));
    }

    /**
     * 获取经度
     *
     * @return 经度
     */
    public static double getLongitude(Context context) {
        return Float.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
    }

    /**
     * 获取纬度
     *
     * @return 纬度
     */
    public static Double getLatitude(Context context) {
        return Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
    }

    /**
     * 获取高度
     *
     * @return 高度
     */
    public static double getHeight(Context context) {
        return Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
    }
}
