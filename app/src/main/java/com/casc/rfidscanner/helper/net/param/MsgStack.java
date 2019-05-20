package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MsgStack {

    private String stage;

    @SerializedName("reader_id")
    private String readerID;

    private double longitude;

    private double latitude;

    private double height;

    @SerializedName("package_time")
    private long time;

    @SerializedName("package_flag")
    private String flag;

    @SerializedName("bucket_list")
    private List<Bucket> buckets = new ArrayList<>();

    public MsgStack(String flag) {
        this.stage = SpHelper.getString(MyParams.S_LINK);
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
        this.time = System.currentTimeMillis();
        this.flag = flag;
    }

    public void addBucket(String epc, long time) {
        buckets.add(new Bucket(epc, time));
    }

    private class Bucket {

        private String epc;

        private long time;

        private Bucket(String epc, long time) {
            this.epc = epc;
            this.time = time;
        }
    }
}
