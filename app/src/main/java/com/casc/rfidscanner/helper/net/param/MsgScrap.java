package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MsgScrap {

    private String stage;

    @SerializedName("reader_id")
    private String readerID;

    private double longitude;

    private double latitude;

    private double height;

    @SerializedName("bucket_list")
    private List<Bucket> buckets = new ArrayList<>();

    public MsgScrap() {
        this.stage = SpHelper.getString(MyParams.S_LINK);
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
    }

    public void addBucket(String epc, String tid, int code) {
        buckets.add(new Bucket(epc, tid, code));
    }

    public void addBucket(String qrcode, int code) {
        buckets.add(new Bucket(qrcode, code));
    }

    private class Bucket {

        private String epc;

        private String tid;

        private long time;

        private String qrcode;

        @SerializedName("disable_code")
        private int code;

        private Bucket(String qrcode, int code) {
            this("", "", qrcode, code);
        }

        private Bucket(String epc, String tid, int code) {
            this(epc, tid, "", code);
        }

        private Bucket(String epc, String tid, String qrcode, int code) {
            this.epc = epc;
            this.tid = tid;
            this.qrcode = qrcode;
            this.time = System.currentTimeMillis();
            this.code = code;
        }
    }
}
