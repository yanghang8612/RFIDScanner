package com.casc.rfidscanner.helper;

import android.support.annotation.NonNull;
import android.util.Log;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.param.MsgAdminLogin;
import com.casc.rfidscanner.helper.param.MsgCardReg;
import com.casc.rfidscanner.helper.param.MsgChkBodyCodeAndTID;
import com.casc.rfidscanner.helper.param.MsgChkStackOrSingle;
import com.casc.rfidscanner.helper.param.MsgCommon;
import com.casc.rfidscanner.helper.param.MsgDealer;
import com.casc.rfidscanner.helper.param.MsgDelivery;
import com.casc.rfidscanner.helper.param.MsgReaderTID;
import com.casc.rfidscanner.helper.param.MsgReflux;
import com.casc.rfidscanner.helper.param.MsgRegister;
import com.casc.rfidscanner.helper.param.MsgScrap;
import com.casc.rfidscanner.helper.param.MsgStack;
import com.casc.rfidscanner.helper.param.MsgUnstack;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
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
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/heartbeat",
                CommonUtils.generateRequestHeader("02"));
    }

    public Call<Reply> getConfig() {
        return netInterface.getConfig(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/parameter",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgReaderTID()));
    }

    public Call<Reply> checkBodyCodeAndTID(MsgChkBodyCodeAndTID params) {
        return netInterface.checkBodyCodeAndTID(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/query",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(params));
    }

    public Call<Reply> checkStackOrSingle(String bucketEPCStr) {
        return netInterface.checkStackOrSingle(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/issinglequery",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgChkStackOrSingle(bucketEPCStr)));
    }

    public Call<Reply> queryDeliveryBill() {
        return netInterface.queryDeliveryBill(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/formquery",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgReaderTID()));
    }

    public Call<Reply> uploadRegisterMsg(MsgRegister msg) {
        return netInterface.uploadRegisterMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadScrapMsg(MsgScrap msg) {
        return netInterface.uploadScrapMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/scrap",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadCommonMsg(MsgCommon msg) {
        return netInterface.uploadCommonMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/common",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadStackMsg(MsgStack msg) {
        return netInterface.uploadStackMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/package",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadDeliveryMsg(MsgDelivery msg) {
        return netInterface.uploadDeliveryMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformout",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadRefluxMsg(MsgReflux msg) {
        return netInterface.uploadRefluxMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformin",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadDealerMsg(MsgDealer msg) {
        String path = "";
        switch (msg.getStage()) {
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
        return netInterface.uploadDealerMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/dealermessage/bucket/" + path,
                CommonUtils.generateRequestHeader("03"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadCardRegMsg(MsgCardReg msg) {
        return netInterface.uploadCardRegMsg(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/card/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(msg));
    }

    public Call<Reply> uploadAdminLoginInfo(String cardEPCStr) {
        return netInterface.uploadAdminLoginInfo(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/login",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgAdminLogin(cardEPCStr)));
    }

    public void uploadUnstackInfo(String bodyCode) {
        netInterface.uploadUnstackInfo(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/unstacker",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(new MsgUnstack(bodyCode))).enqueue(new Callback<Reply>() {
            @Override
            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                Reply reply = response.body();
                if (!response.isSuccessful() || reply == null || reply.getCode() != 200) {
                    Log.i(TAG, "Unstack error: " + Objects.requireNonNull(reply).getContent());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                Log.i(TAG, "Unstack error: Net error.");
            }
        });
    }
}
