package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.Arrays;

public class Bucket {

    private byte[] epc;

    private byte[] tid;

    private long time;

    private ProductInfo info;

    private String bodyCode = "";

    public Bucket(byte[] epc) {
        this.epc = epc;
        this.time = System.currentTimeMillis();
        this.info = MyVars.config.getProductInfoByCode(epc[5]);
        for (int i = 0; i < 5; i++) {
            this.bodyCode = String.format("%s%s", this.bodyCode, (char) epc[7 + i]);
        }
        this.bodyCode = MyVars.config.getHeader() + this.bodyCode;
    }

    public Bucket(String epcStr) {
        this(CommonUtils.hexToBytes(epcStr));
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
