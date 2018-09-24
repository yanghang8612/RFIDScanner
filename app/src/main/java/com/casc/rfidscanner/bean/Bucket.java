package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.Arrays;

public class Bucket {

    public static ProductInfo getProductInfo(String epcStr) {
        return getProductInfo(CommonUtils.hexToBytes(epcStr));
    }

    public static ProductInfo getProductInfo(byte[] epc) {
        return MyVars.config.getProductInfoByCode(epc[5]);
    }

    public static String getBodyCode(String epcStr) {
        return getBodyCode(CommonUtils.hexToBytes(epcStr));
    }

    public static String getBodyCode(byte[] epc) {
        String res = "";
        for (int i = 0; i < 5; i++) {
            res = String.format("%s%s", res, (char) epc[7 + i]);
        }
        return MyVars.config.getHeader() + res;
    }

    private byte[] epc;

    private byte[] tid;

    private long time;

    private ProductInfo info;

    private String bodyCode;

    private String flag;

    public Bucket(byte[] epc, String flag) {
        this.epc = epc;
        this.flag = flag;
        this.time = System.currentTimeMillis();
        this.info = MyVars.config.getProductInfoByCode(epc[5]);
        this.bodyCode = "";
        for (int i = 0; i < 5; i++) {
            this.bodyCode = String.format("%s%s", this.bodyCode, (char) epc[7 + i]);
        }
        this.bodyCode = MyVars.config.getHeader() + this.bodyCode;
    }

    public Bucket(String epcStr, String flag) {
        this(CommonUtils.hexToBytes(epcStr), flag);
    }

    public Bucket(byte[] epc) {
        this(epc, "1");
    }

    public Bucket(String epcStr, long time) {
        this(CommonUtils.hexToBytes(epcStr));
        this.time = time;
    }

    public boolean isScraped() {
        return epc[MyParams.EPC_TYPE_INDEX] == MyParams.EPCType.BUCKET_SCRAPED.getCode();
    }

    public void setScraped() {
        epc[MyParams.EPC_TYPE_INDEX] = MyParams.EPCType.BUCKET_SCRAPED.getCode();
    }

    public String getKey() {
        return flag + getEpcStr();
    }

    public byte[] getEpc() {
        return epc;
    }

    public String getEpcStr() {
        return CommonUtils.bytesToHex(epc);
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

    public ProductInfo getProductInfo() {
        return info;
    }

    public int getCode() {
        return info.getCode();
    }

    public String getName() {
        return info.getName();
    }

    public String getBodyCode() {
        return bodyCode;
    }

    public String getFlag() {
        return flag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Bucket bucket = (Bucket) o;
        return Arrays.equals(epc, bucket.epc);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(epc);
    }
}
