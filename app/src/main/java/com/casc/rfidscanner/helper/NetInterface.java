package com.casc.rfidscanner.helper;

import com.casc.rfidscanner.helper.param.Reply;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public interface NetInterface {

    @GET
    Call<Reply> sendHeartbeat(@Url String url, @QueryMap Map<String, String> header);

    @POST
    Call<Reply> getConfig(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody params);

    @POST
    Call<Reply> checkBodyCodeAndTID(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody params);

    @POST
    Call<Reply> checkStackOrSingle(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody params);

    @POST
    Call<Reply> queryDeliveryBill(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody params);

    @POST
    Call<Reply> uploadRegisterMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadScrapMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadCommonMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadStackMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadDeliveryMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadRefluxMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadDealerMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadCardRegMsg(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody msg);

    @POST
    Call<Reply> uploadAdminLoginInfo(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody info);

    @POST
    Call<Reply> uploadUnstackInfo(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody info);

    @POST
    Call<Reply> reportHeartbeat(@Url String url, @Query("line") String line);
}
