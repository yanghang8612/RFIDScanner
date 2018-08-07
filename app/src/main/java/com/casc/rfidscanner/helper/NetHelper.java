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
                CommonUtils.generateRequestBody(query));
    }

    public Call<Reply> uploadR0Message(MessageRegister r0) {
        return netInterface.uploadR0Message(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(r0));
    }

    public Call<Reply> uploadR1Message(MessageScrap r1) {
        return netInterface.uploadR1Message(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/scrap",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(r1));
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
                CommonUtils.generateRequestBody(config));
    }

    public Call<Reply> sendHeartbeat() {
        return netInterface.sendHeartbeat(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/heartbeat",
                CommonUtils.generateRequestHeader("02"));
    }

    public Call<Reply> uploadCardRegMessage(MessageCardReg card) {
        return netInterface.uploadCardRegMessage(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/device/card/register",
                CommonUtils.generateRequestHeader("02"),
                CommonUtils.generateRequestBody(card));
    }

    public Call<Reply> uploadAdminLoginInfo(RequestBody login) {
        return netInterface.uploadAdminLoginInfo(
                ConfigHelper.getParam(MyParams.S_MAIN_PLATFORM_ADDR) + "/api/message/bucket/login",
                CommonUtils.generateRequestHeader("02"), login);
    }

    public void reportHeartbeat() {
        netInterface.reportHeartbeat(
                ConfigHelper.getParam(MyParams.S_MONITOR_APP_ADDR) + "/heartbeat",
                ConfigHelper.getParam(MyParams.S_LINE_NAMME))
                .enqueue(new BillCallback());
    }

    public void reportBillDelivery(MessageBillDelivery bill) {
        netInterface.reportBillDelivery(
                ConfigHelper.getParam(MyParams.S_MONITOR_APP_ADDR) + "/bill_delivery",
                ConfigHelper.getParam(MyParams.S_LINE_NAMME),
                CommonUtils.generateRequestBody(bill)).enqueue(new BillCallback());
    }

    public void reportBillReflux(MessageBillDelivery bill) {
        netInterface.reportBillReflux(
                ConfigHelper.getParam(MyParams.S_MONITOR_APP_ADDR) + "/bill_reflux",
                ConfigHelper.getParam(MyParams.S_LINE_NAMME),
                CommonUtils.generateRequestBody(bill)).enqueue(new BillCallback());
    }

    public void reportBillBucket(MessageBillBucket bucket) {
        netInterface.reportBillBucket(
                ConfigHelper.getParam(MyParams.S_MONITOR_APP_ADDR) + "/bill_bucket",
                ConfigHelper.getParam(MyParams.S_LINE_NAMME),
                CommonUtils.generateRequestBody(bucket)).enqueue(new BillCallback());
    }

    public void reportBillComplete(MessageBillComplete message) {
        netInterface.reportBillComplete(
                ConfigHelper.getParam(MyParams.S_MONITOR_APP_ADDR) + "/bill_complete",
                ConfigHelper.getParam(MyParams.S_LINE_NAMME),
                CommonUtils.generateRequestBody(message)).enqueue(new BillCallback());
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
