package com.casc.rfidscanner.helper.param;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Reply {

    // 平台响应状态码，参见外部接口协议中关于此字段的定义
    private int code;

    // 平台响应消息
    private String message;

    // 与请求中匹配的随机数
    private int random_number;

    // 平台响应内容主体
    private JsonElement content;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getRandom_number() {
        return random_number;
    }

    public void setRandom_number(int random_number) {
        this.random_number = random_number;
    }

    public JsonElement getContent() {
        return content;
    }

    public void setContent(JsonObject content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "code:" + code +
                ",message:" + message +
                ",random_number:" + random_number +
                ",content:" + content;
    }
}
