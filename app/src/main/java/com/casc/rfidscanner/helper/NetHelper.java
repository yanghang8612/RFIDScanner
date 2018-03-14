package com.casc.rfidscanner.helper;

import android.support.annotation.NonNull;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.param.MessageCardReg;
import com.casc.rfidscanner.helper.param.MessageCommon;
import com.casc.rfidscanner.helper.param.MessageConfig;
import com.casc.rfidscanner.helper.param.MessageDealer;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetHelper implements Callback<Reply> {

    private final NetInterface netInterface;

    private static class SingletonHolder{
        private static final NetHelper instance = new NetHelper();
    }

    private NetHelper() {
        this.netInterface = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR))
                .client(new OkHttpClient.Builder()
                        .connectTimeout(MyParams.NET_CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                        .writeTimeout(MyParams.NET_RW_TIMEOUT, TimeUnit.MILLISECONDS)
                        .readTimeout(MyParams.NET_RW_TIMEOUT, TimeUnit.MILLISECONDS)
                        .build())
                .build()
                .create(NetInterface.class);
    }

    public static NetHelper getInstance() {
        return SingletonHolder.instance;
    }

    public Call<Reply> checkBodyCodeAndTID(MessageQuery query) {
        return netInterface.checkBodyCodeAndTID(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/query",
                CommonUtils.generateRequestHeader("02"),
                query);
    }

    public Call<Reply> uploadR0Message(MessageRegister r0) {
        return netInterface.uploadR0Message(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/register",
                CommonUtils.generateRequestHeader("02"),
                r0);
    }

    public Call<Reply> uploadCommonMessage(MessageCommon common) {
        return netInterface.uploadCommonMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/common",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(common));
    }

    public Call<Reply> uploadDeliveryMessage(MessageDelivery delivery) {
        return netInterface.uploadDeliveryMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformout",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(delivery));
    }

    public Call<Reply> uploadRefluxMessage(MessageReflux reflux) {
        return netInterface.uploadRefluxMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformin",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(reflux));
    }

    public Call<Reply> uploadDealerMessage(MessageDealer dealer) {
        String path = "";
        switch (dealer.getStage()) {
            case "10":
                path = "fullelecformin";
                break;
            case "11":
                path = "fullelecformout";
                break;
            case "12":
                path = "emptyelecformin";
                break;
            case "13":
                path = "emptyelecformout";
                break;
        }
        return netInterface.uploadDealerMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/dealermessage/bucket/" + path,
                CommonUtils.generateRequestHeader("03"),
                CommonUtils.generateRequestBody(dealer));
    }

    public Call<Reply> getConfig(MessageConfig config) {
        return netInterface.getConfig(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/parameter",
                CommonUtils.generateRequestHeader("02"),
                config);
    }

    public Call<Reply> sendHeartbeat() {
        return netInterface.sendHeartbeat(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/heartbeat",
                CommonUtils.generateRequestHeader("02"));
    }

    public Call<Reply> uploadCardRegMessage(MessageCardReg card) {
        return netInterface.uploadCardRegMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/card/register",
                CommonUtils.generateRequestHeader("02"), card);
    }

    public Call<Reply> uploadAdminLoginInfo(RequestBody login) {
        return netInterface.uploadAdminLoginInfo(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/login",
                CommonUtils.generateRequestHeader("02"), login);
    }

    @Override
    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {}

    @Override
    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {}
}
