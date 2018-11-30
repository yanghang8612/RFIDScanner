package com.casc.rfidscanner.helper;

import com.casc.rfidscanner.helper.param.Reply;

import java.util.Map;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

public interface NetInterface {

    @GET
    Call<Reply> sendHeartbeat(@Url String url, @QueryMap Map<String, String> header);

    @POST
    Call<Reply> post(@Url String url, @QueryMap Map<String, String> header, @Body RequestBody body);

}
