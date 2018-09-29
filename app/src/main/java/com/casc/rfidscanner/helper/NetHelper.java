package com.casc.rfidscanner.helper;

import android.support.annotation.NonNull;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.helper.param.MessageBillBucket;
import com.casc.rfidscanner.helper.param.MessageBillComplete;
import com.casc.rfidscanner.helper.param.MessageBillDelivery;
import com.casc.rfidscanner.helper.param.MessageCardReg;
import com.casc.rfidscanner.helper.param.MessageCommon;
import com.casc.rfidscanner.helper.param.MessageConfig;
import com.casc.rfidscanner.helper.param.MessageDealer;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.MessageScrap;
import com.casc.rfidscanner.helper.param.MessageStack;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.ResendAllBillsMessage;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
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

    public Call<Reply> checkBodyCodeAndTID(MessageQuery query) {
        return netInterface.checkBodyCodeAndTID(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/query",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(query));
    }

    public Call<Reply> uploadRegisterMessage(MessageRegister register) {
        return netInterface.uploadRegisterMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(register));
    }

    public Call<Reply> uploadScrapMessage(MessageScrap scrap) {
        return netInterface.uploadScrapMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/scrap",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(scrap));
    }

    public Call<Reply> uploadCommonMessage(MessageCommon common) {
        return netInterface.uploadCommonMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/common",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(common));
    }

    public Call<Reply> uploadStackMessage(MessageStack stack) {
        return netInterface.uploadStackMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/package",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(stack));
    }

    public Call<Reply> uploadDeliveryMessage(MessageDelivery delivery) {
        return netInterface.uploadDeliveryMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformout",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(delivery));
    }

    public Call<Reply> uploadRefluxMessage(MessageReflux reflux) {
        return netInterface.uploadRefluxMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/elecformin",
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
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/dealermessage/bucket/" + path,
                CommonUtils.generateRequestHeader("03"),
                CommonUtils.generateRequestBody(dealer));
    }

    public Call<Reply> getConfig(MessageConfig config) {
        return netInterface.getConfig(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/parameter",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(config));
    }

    public Call<Reply> sendHeartbeat() {
        return netInterface.sendHeartbeat(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/heartbeat",
                CommonUtils.generateRequestHeader("02"));
    }

    public Call<Reply> uploadCardRegMessage(MessageCardReg card) {
        return netInterface.uploadCardRegMessage(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/card/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(card));
    }

    public Call<Reply> uploadAdminLoginInfo(RequestBody login) {
        return netInterface.uploadAdminLoginInfo(
                ConfigHelper.getString(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/login",
                CommonUtils.generateRequestHeader("02"), login);
    }

    public void reportHeartbeat() {
        if (MyParams.MONITOR_ON) {
            netInterface.reportHeartbeat(
                    ConfigHelper.getString(MyParams.S_MONITOR_APP_ADDR) + "/heartbeat",
                    ConfigHelper.getString(MyParams.S_LINE_NAME))
                    .enqueue(new BillCallback());
        }
    }

    public void reportBillDelivery(MessageBillDelivery bill) {
        if (MyParams.MONITOR_ON) {
            netInterface.reportBillDelivery(
                    ConfigHelper.getString(MyParams.S_MONITOR_APP_ADDR) + "/bill_delivery",
                    ConfigHelper.getString(MyParams.S_LINE_NAME),
                    CommonUtils.generateRequestBody(bill)).enqueue(new BillCallback());
        }
    }

    public void reportBillReflux(MessageBillDelivery bill) {
        if (MyParams.MONITOR_ON) {
            netInterface.reportBillReflux(
                    ConfigHelper.getString(MyParams.S_MONITOR_APP_ADDR) + "/bill_reflux",
                    ConfigHelper.getString(MyParams.S_LINE_NAME),
                    CommonUtils.generateRequestBody(bill)).enqueue(new BillCallback());
        }
    }

    public void reportBillBucket(MessageBillBucket bucket) {
        if (MyParams.MONITOR_ON) {
            netInterface.reportBillBucket(
                    ConfigHelper.getString(MyParams.S_MONITOR_APP_ADDR) + "/bill_bucket",
                    ConfigHelper.getString(MyParams.S_LINE_NAME),
                    CommonUtils.generateRequestBody(bucket)).enqueue(new BillCallback());
        }
    }

    public void reportBillComplete(MessageBillComplete message) {
        if (MyParams.MONITOR_ON) {
            netInterface.reportBillComplete(
                    ConfigHelper.getString(MyParams.S_MONITOR_APP_ADDR) + "/bill_complete",
                    ConfigHelper.getString(MyParams.S_LINE_NAME),
                    CommonUtils.generateRequestBody(message)).enqueue(new BillCallback());
        }
    }

    private class BillCallback implements Callback<Reply> {

        @Override
        public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
            Reply reply = response.body();
            if (!response.isSuccessful()) { // 如果出现有响应但没成功，证明监控Server在线，但是处理异常，重新发送
                call.clone().enqueue(this);
            } else if (reply != null) { // 正常从服务端获得响应，更新MyVars中的server更新时间以及LineName
                MyVars.server.update();
                MyVars.server.setLineName(reply.getMessage());
                if (reply.getCode() == 201) { // code:201，说明是新客户端
                    EventBus.getDefault().post(new ResendAllBillsMessage());
                }
            }
        }

        @Override
        public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
            //t.printStackTrace();
        }
    }
}
