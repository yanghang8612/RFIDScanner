package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MessageOnline {

    private String stage;

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    private long starttime;

    private long endtime;

    private String productname;

    private int productnumber;

    private int amount;

    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageOnline(String epcStr) {
        long curTime = System.currentTimeMillis();
        this.stage = ConfigHelper.getString(MyParams.S_LINK);
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getString(MyParams.S_HEIGHT));
        this.starttime = curTime;
        this.endtime = curTime;
        this.addBucket(curTime, epcStr);
    }

    public MessageOnline(String productname, int productnumber, int amount) {
        this.stage = ConfigHelper.getString(MyParams.S_LINK);
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getString(MyParams.S_HEIGHT));
        this.starttime = System.currentTimeMillis();
        this.productname = productname;
        this.productnumber = productnumber;
        this.amount = amount;
    }

    public MessageOnline setEndTime(long endtime) {
        this.endtime = endtime;
        return this;
    }

    public String getProductname() {
        return productname;
    }

    public MessageOnline addBucket(long time, String tid, String epc) {
        bucket_info.add(new Bucket(time, tid, epc));
        return this;
    }

    public MessageOnline addBucket(long time, String epc) {
        bucket_info.add(new Bucket(time, "", epc));
        return this;
    }

    public MessageOnline addBucket(String epc) {
        bucket_info.add(new Bucket(System.currentTimeMillis(), "", epc));
        return this;
    }

    // 桶信息的内部类
    private class Bucket {

        private long bucket_time;

        private String bucket_TID;

        private String bucket_epc;

        private Bucket(long time, String tid, String epc) {
            this.bucket_time = time;
            this.bucket_TID = tid;
            this.bucket_epc = epc;
        }
    }
}
