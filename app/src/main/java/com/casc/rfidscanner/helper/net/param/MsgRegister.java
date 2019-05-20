package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MsgRegister {

    private String stage;

    @SerializedName("reader_id")
    private String readerID;

    private double longitude;

    private double latitude;

    private double height;

    @SerializedName("product_name")
    private String productName;

    @SerializedName("bucket_list")
    private List<BucketInfo> buckets = new ArrayList<>();

    public MsgRegister(String productName) {
        this.stage = SpHelper.getString(MyParams.S_LINK);
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
        this.productName = productName;
    }

    public void addBucket(String epc, String tid, String code) {
        buckets.add(new BucketInfo(epc, tid, code));
    }

    // 桶信息的内部类
    private class BucketInfo {

        private String epc;

        private String tid;

        private long time;

        private String qrcode;

        public BucketInfo(String epc, String tid, String qrcode) {
            this.epc = epc;
            this.tid = tid;
            this.time = System.currentTimeMillis();
            this.qrcode = qrcode;
        }
    }
}
