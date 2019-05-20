package com.casc.rfidscanner.helper.net.param;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import java.util.Random;

public class Reply {

    // 平台响应状态码，参见外部接口协议中关于此字段的定义
    private int code;

    // 平台响应消息
    private String message;

    // 与请求中匹配的随机数
    @SerializedName("random_number")
    private int randomNumber;

    // 平台响应内容主体
    private JsonElement content;

    public Reply(int code, String message) {
        this.code = code;
        this.message = message;
        this.randomNumber = new Random().nextInt();
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getRandomNumber() {
        return randomNumber;
    }

    public JsonElement getContent() {
        return content;
    }

    @Override
    public String toString() {
        return "code:" + code +
                ",message:" + message +
                ",random_number:" + randomNumber +
                ",content:" + content;
    }
}
