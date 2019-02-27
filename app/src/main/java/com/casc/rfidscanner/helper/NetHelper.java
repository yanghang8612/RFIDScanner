package com.casc.rfidscanner.helper;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.helper.param.MsgAdminLogin;
import com.casc.rfidscanner.helper.param.MsgCardReg;
import com.casc.rfidscanner.helper.param.MsgChkBodyCodeAndTID;
import com.casc.rfidscanner.helper.param.MsgChkStack;
import com.casc.rfidscanner.helper.param.MsgCommon;
import com.casc.rfidscanner.helper.param.MsgDealer;
import com.casc.rfidscanner.helper.param.MsgDelivery;
import com.casc.rfidscanner.helper.param.MsgLog;
import com.casc.rfidscanner.helper.param.MsgOnline;
import com.casc.rfidscanner.helper.param.MsgReaderTID;
import com.casc.rfidscanner.helper.param.MsgReflux;
import com.casc.rfidscanner.helper.param.MsgRegister;
import com.casc.rfidscanner.helper.param.MsgRfTID;
import com.casc.rfidscanner.helper.param.MsgScrap;
import com.casc.rfidscanner.helper.param.MsgStack;
import com.casc.rfidscanner.helper.param.MsgTask;
import com.casc.rfidscanner.helper.param.MsgUnstack;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.utils.CommonUtils;

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
                .baseUrl(ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR))
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

    public Call<Reply> sendHeartbeat() {
        return netInterface.sendHeartbeat(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/platform/status",
                CommonUtils.generateRequestHeader("02"));
    }

    public Call<Reply> getApiConfig() {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/sq/configs/pad",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgReaderTID()));
    }

    public Call<Reply> uploadCardRegMsg(MsgCardReg msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/card/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadAdminLoginMsg(String cardEPCStr) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/products",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgAdminLogin(cardEPCStr)));
    }

    public Call<Reply> uploadLogMsg(MsgLog msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_STANDBY_PLATFORM_ADDR) + "/log",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> getConfig() {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/sq/configs/pad",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgReaderTID()));
    }

    public Call<Reply> uploadRegisterMsg(MsgRegister msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/buckets",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> checkBodyCodeAndTID(MsgChkBodyCodeAndTID params) {
        return netInterface.get(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/buckets",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(params));
    }

    public Call<Reply> uploadScrapMsg(MsgScrap msg) {
        return netInterface.delete(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/buckets",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadProductMsg(MsgCommon msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/products",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadDeliveryMsg(MsgDelivery msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/delivery_receipts",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadReturnMsg(MsgDelivery msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/return_receipts",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadRefluxMsg(MsgReflux msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/reflux_receipts",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> queryDeliveryBill() {
        return netInterface.get(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/assets/delivery_forms",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgReaderTID()));
    }

    public Call<Reply> uploadLineMsg(MsgCommon msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/produce/line_messages",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadStackMsg(MsgStack msg) {
        return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/produce/packages",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> checkStackOrSingle(String bucketEPCStr) {
        return netInterface.get(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR)
                        + "/api/sq/" + MyVars.config.getCompanySymbol() + "/produce/packages",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgChkStack(bucketEPCStr)));
    }

    public Call<Reply> uploadUnstackInfo(String bodyCode) {
         return netInterface.post(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/unstacker",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgUnstack(bodyCode)));
    }
}
