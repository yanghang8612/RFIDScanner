package com.casc.rfidscanner.helper;

import android.support.annotation.NonNull;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.param.MessageCardReg;
import com.casc.rfidscanner.helper.param.MessageCommon;
import com.casc.rfidscanner.helper.param.MessageConfig;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.utils.CommonUtils;
import com.google.gson.Gson;

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
                .build()
                .create(NetInterface.class);
    }

    public static NetHelper getInstance() {
        return SingletonHolder.instance;
    }

    public Call<Reply> checkBodyCodeAndTID(MessageQuery query) {
        return netInterface.checkBodyCodeAndTID(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/query",
                CommonUtils.generateRequestHeader(),
                query);
    }

    public Call<Reply> uploadR0Message(MessageRegister r0) {
        netInterface.cacheData(ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/cache", r0.getStage(),
                "/api/message/bucket/common", new Gson().toJson(r0))
                .enqueue(this);
        return netInterface.uploadR0Message(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/register",
                CommonUtils.generateRequestHeader(),
                r0);
    }

    public Call<Reply> uploadCommonMessage(MessageCommon common) {
        netInterface.cacheData(ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/cache", common.getStage(),
                "/api/message/bucket/common", new Gson().toJson(common))
                .enqueue(this);
        return netInterface.uploadCommonMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/common",
                CommonUtils.generateRequestHeader(),
                CommonUtils.generateRequestBody(common));
    }

    public Call<Reply> uploadDeliveryMessage(MessageDelivery delivery) {
        netInterface.cacheData(ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/cache", delivery.getStage(),
                "/api/message/bucket/elecformout", new Gson().toJson(delivery))
                .enqueue(this);
        return netInterface.uploadDeliveryMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformout",
                CommonUtils.generateRequestHeader(),
                CommonUtils.generateRequestBody(delivery));
    }

    public Call<Reply> uploadRefluxMessage(MessageReflux reflux) {
        netInterface.cacheData(ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/cache", reflux.getStage(),
                "/api/message/bucket/elecformin", new Gson().toJson(reflux))
                .enqueue(this);
        return netInterface.uploadRefluxMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformin",
                CommonUtils.generateRequestHeader(),
                CommonUtils.generateRequestBody(reflux));
    }

    public Call<Reply> getConfig(MessageConfig config) {
        return netInterface.getConfig(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/parameter",
                CommonUtils.generateRequestHeader(),
                config);
    }

    public Call<Reply> sendHeartbeat() {
        return netInterface.sendHeartbeat(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/heartbeat",
                CommonUtils.generateRequestHeader());
    }

    public Call<Reply> uploadCardRegMessage(MessageCardReg card) {
        return netInterface.uploadCardRegMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/card/register",
                CommonUtils.generateRequestHeader(), card);
    }

    public Call<Reply> uploadAdminLoginInfo(RequestBody login) {
        return netInterface.uploadAdminLoginInfo(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/login",
                CommonUtils.generateRequestHeader(), login);
    }

    @Override
    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {}

    @Override
    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {}
}
