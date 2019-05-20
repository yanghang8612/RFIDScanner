package com.casc.rfidscanner.helper;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.helper.net.NetInterface;
import com.casc.rfidscanner.helper.net.param.MsgAdminLogin;
import com.casc.rfidscanner.helper.net.param.MsgCardReg;
import com.casc.rfidscanner.helper.net.param.MsgLine;
import com.casc.rfidscanner.helper.net.param.MsgDelivery;
import com.casc.rfidscanner.helper.net.param.MsgLog;
import com.casc.rfidscanner.helper.net.param.MsgReflux;
import com.casc.rfidscanner.helper.net.param.MsgRegister;
import com.casc.rfidscanner.helper.net.param.MsgScrap;
import com.casc.rfidscanner.helper.net.param.MsgStack;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetHelper {

    private final String TAG = NetHelper.class.getSimpleName();

    private final NetInterface netInterface;

    private static class SingletonHolder{
        private static final NetHelper instance = new NetHelper();
    }

    private NetHelper() {
        this.netInterface = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(SpHelper.getString(MyParams.S_DEVICE_ADDR))
                .client(new OkHttpClient.Builder()
                        .connectTimeout(MyParams.NET_CONNECT_TIMEOUT, TimeUnit.SECONDS)
                        .writeTimeout(MyParams.NET_RW_TIMEOUT, TimeUnit.SECONDS)
                        .readTimeout(MyParams.NET_RW_TIMEOUT, TimeUnit.SECONDS)
                        .build())
                .build()
                .create(NetInterface.class);
    }

    public static NetHelper getInstance() {
        return SingletonHolder.instance;
    }

    /**
     * 以下为运管云的接口
     */
    public Call<Reply> sendHeartbeat() {
        return netInterface.get(SpHelper.getString(MyParams.S_DEVICE_ADDR) + "/api/platform/status",
                CommonUtils.generateRequestHeader("02"), new HashMap<String, Object>());
    }

    public Call<Reply> getApiConfig() {
        Map<String, Object> map = new HashMap<>();
        map.put("soft_id", SpHelper.getString(MyParams.S_READER_ID));
        map.put("soft_type", MyParams.SOFT_CODE);
        return netInterface.get(SpHelper.getString(MyParams.S_DEVICE_ADDR) + "/api/device/configs",
                CommonUtils.generateRequestHeader("02"), map);
    }

    public Call<Reply> uploadLogMsg(MsgLog msg) {
        return netInterface.post(SpHelper.getString(MyParams.S_DEVICE_ADDR) + "/api/device/logs",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadAdminLoginMsg(String epcStr) {
        return netInterface.post(SpHelper.getString(MyParams.S_DEVICE_ADDR) + "/api/device/login_records/sqpad",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(new MsgAdminLogin(epcStr)));
    }

    public Call<Reply> uploadCardRegMsg(MsgCardReg msg) {
        return netInterface.post(SpHelper.getString(MyParams.S_DEVICE_ADDR) + "/api/device/cards",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    /**
     * 以下为数据云的接口
     */
    public Call<Reply> getConfig() {
        return netInterface.get(MyVars.api.getDataCloudAddr() + "/api/sq/configs/pad",
                CommonUtils.generateRequestHeader("02"), generateReaderIDMap());
    }

    public Call<Reply> uploadRegisterMsg(MsgRegister msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/assets/buckets",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> getRegisterInfo(String tid, String qrcode) {
        Map<String, Object> map = new HashMap<>();
        map.put("tid", tid);
        map.put("qrcode", qrcode);
        return netInterface.get(MyVars.api.getDataCloudAddr() + "/api/sq/assets/buckets",
                CommonUtils.generateRequestHeader("02"), map);
    }

    public Call<Reply> uploadScrapMsg(MsgScrap msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/assets/scrap",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadProductMsg(MsgLine msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/assets/products",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadDeliveryMsg(MsgDelivery msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/assets/delivery_receipts",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadRefluxMsg(MsgReflux msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/assets/reflux_receipts",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> queryDeliveryBills() {
        return netInterface.get(MyVars.api.getDataCloudAddr() + "/api/sq/assets/delivery_forms",
                CommonUtils.generateRequestHeader("02"), generateReaderIDMap());
    }

    public Call<Reply> uploadLineMsg(MsgLine msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/produce/line_messages",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadStackMsg(MsgStack msg) {
        return netInterface.post(MyVars.api.getDataCloudAddr() + "/api/sq/produce/packages",
                CommonUtils.generateRequestHeader("02"), CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> checkStackOrSingle(String epcStr) {
        Map<String, Object> map = new HashMap<>();
        map.put("bucket_epc", epcStr);
        return netInterface.get(MyVars.api.getDataCloudAddr() + "/api/sq/produce/packages",
                CommonUtils.generateRequestHeader("02"), map);
    }

    private Map<String, Object> generateReaderIDMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("reader_id", SpHelper.getString(MyParams.S_READER_ID));
        return map;
    }
}
