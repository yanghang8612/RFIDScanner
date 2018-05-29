package com.casc.rfidscanner.bean;

public class DisableInfo {

    private int disablecode;

    private String disableword;

    public int getCode() {
        return disablecode;
    }

    public String getWord() {
        return disableword;
    }

    @Override
    public String toString() {
        return disableword;
    }
}
