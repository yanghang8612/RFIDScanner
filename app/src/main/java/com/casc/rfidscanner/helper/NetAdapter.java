package com.casc.rfidscanner.helper;

import android.support.annotation.NonNull;

import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.NetErrorMessage;

import org.greenrobot.eventbus.EventBus;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public abstract class NetAdapter implements Callback<Reply> {

    public abstract void onSuccess(Reply reply);

    public abstract void onFail();

    @Override
    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
        Reply reply = response.body();
        if (response.isSuccessful() && reply != null) {
            if (reply.getCode() == 200) {
                onSuccess(reply);
            } else {
                onFail();
                EventBus.getDefault().post(new NetErrorMessage(reply.getMessage()));
            }
        } else {
            onFail();
            EventBus.getDefault().post(new NetErrorMessage("平台内部错误" + response.code() + ",请稍后重试"));
        }
    }

    @Override
    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
        onFail();
        EventBus.getDefault().post(new NetErrorMessage("网络连接失败,请检查后重试"));
    }
}
