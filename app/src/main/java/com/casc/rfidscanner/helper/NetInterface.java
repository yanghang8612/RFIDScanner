package com.casc.rfidscanner.helper;

import com.casc.rfidscanner.helper.param.MessageCardReg;
import com.casc.rfidscanner.helper.param.MessageConfig;
import com.casc.rfidscanner.helper.param.MessageQuery;
import com.casc.rfidscanner.helper.param.MessageRegister;
import com.casc.rfidscanner.helper.param.MessageScrap;
import com.casc.rfidscanner.helper.param.Reply;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public interface NetInterface {

    @POST
    Call<Reply> checkBodyCodeAndTID(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody query);

    @POST
    Call<Reply> uploadR0Message(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody r0);

    @POST
    Call<Reply> uploadR1Message(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody r1);

    @POST
    Call<Reply> uploadCommonMessage(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody common);

    @POST
    Call<Reply> uploadDeliveryMessage(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody delivery);

    @POST
    Call<Reply> uploadRefluxMessage(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody reflux);

    @POST
    Call<Reply> uploadDealerMessage(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody dealer);

    @POST
    Call<Reply> getConfig(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody config);

    @GET
    Call<Reply> sendHeartbeat(@Url String url, @QueryMap Map<String, String> header);

    @POST
    Call<Reply> uploadCardRegMessage(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody card);

    @POST
    Call<Reply> uploadAdminLoginInfo(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody login);

    @POST
    @FormUrlEncoded
    Call<Reply> cacheData(@Url String url, @Field("stage") String stage, @Field("path") String path, @Field("content") String content);
}
