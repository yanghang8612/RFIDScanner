package com.casc.rfidscanner.bean;

public class Specify {

    private byte code;

    private String specify;

    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public String getSpecify() {
        return specify;
    }

    public void setSpecify(String specify) {
        this.specify = specify;
    }

    @Override
    public String toString() {
        return specify;
    }
}
