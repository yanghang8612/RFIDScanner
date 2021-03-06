package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class MsgReflux {

    private String stage;

    @SerializedName("reader_id")
    private String readerID;

    private double longitude;

    private double latitude;

    private double height;

    @SerializedName("confirm_time")
    private long confirmTime;

    @SerializedName("dealer_name")
    private String dealer;

    @SerializedName("driver_name")
    private String driver;

    @SerializedName("unknown_count")
    private int unknownCount;

    @SerializedName("bucket_list")
    private List<Bucket> buckets = new ArrayList<>();

    public MsgReflux(String dealer, String driver, int unknownCount) {
        this.stage = SpHelper.getString(MyParams.S_LINK);
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
        this.confirmTime = System.currentTimeMillis();
        this.dealer = dealer;
        this.driver = driver;
        this.unknownCount = unknownCount;
    }

    public void addBucket(String epc, long time, String qrcode) {
        buckets.add(new Bucket(epc, time, qrcode));
    }

    private class Bucket {

        private String epc;

        private long time;

        private String qrcode;

        private Bucket(String epc, long time, String qrcode) {
            this.epc = epc;
            this.time = time;
            this.qrcode = qrcode;
        }
    }
}
