package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MsgLine {

    private String stage;

    @SerializedName("reader_id")
    private String readerID;

    private double longitude;

    private double latitude;

    private double height;

    @SerializedName("bucket_list")
    private List<Bucket> buckets = new ArrayList<>();

    public MsgLine() {
        this.stage = SpHelper.getString(MyParams.S_LINK);
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
    }

    public MsgLine addBucket(String epc) {
        buckets.add(new Bucket(epc));
        return this;
    }

    private class Bucket {

        private String epc;

        private long time;

        private Bucket(String epc) {
            this.epc = epc;
            this.time = System.currentTimeMillis();
        }
    }
}
