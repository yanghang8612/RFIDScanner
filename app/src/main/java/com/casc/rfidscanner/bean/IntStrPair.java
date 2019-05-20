package com.casc.rfidscanner.bean;

import com.google.gson.annotations.SerializedName;

public class IntStrPair {

    @SerializedName(value = "code", alternate = {"quantity"})
    private int intValue;

    @SerializedName(value = "name", alternate = {"reason"})
    private String strValue;

    public int getInt() {
        return intValue;
    }

    public String getStr() {
        return strValue;
    }

    @Override
    public String toString() {
        return strValue;
    }
}
