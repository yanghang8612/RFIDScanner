package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.utils.CommonUtils;

public class Bucket {

    private byte[] epc;

    private byte[] tid;

    private long time;

    private String bodyCode;

    private String bucketSpec;

    private String waterBrand;

    private String waterSpec;

    public Bucket(byte[] epc) {
        this.epc = epc;
        this.time = System.currentTimeMillis();
        this.bucketSpec = MyVars.config.getBucketSpecByCode(epc[10]);
        this.waterBrand = MyVars.config.getWaterBrandByCode(epc[11]);
        this.waterSpec = MyVars.config.getWaterSpecByCode(epc[12]);
        int code = 0;
        code += ((epc[13] & 0xFF) << 16);
        code += ((epc[14] & 0xFF) << 8);
        code += (epc[15] & 0xFF);
        this.bodyCode = MyVars.config.getCompanySymbol() + String.format("%06d", code);
    }

    public Bucket(String epcStr) {
        this(CommonUtils.hexToBytes(epcStr));
    }

    public byte[] getEpc() {
        return epc;
    }

    public byte[] getTid() {
        return tid;
    }

    public void setTid(byte[] tid) {
        this.tid = tid;
    }

    public long getTime() {
        return time;
    }

    public String getBodyCode() {
        return bodyCode;
    }

    public String getBucketSpec() {
        return bucketSpec;
    }

    public String getWaterBrand() {
        return waterBrand;
    }

    public String getWaterSpec() {
        return waterSpec;
    }
}
