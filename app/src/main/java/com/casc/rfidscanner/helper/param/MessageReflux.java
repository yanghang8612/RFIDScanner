package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageReflux {

    private String stage;

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    private long time;

    private String dealer;

    private String driver;

    private int unknown;

    // 成品出库的桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageReflux(String dealer, String driver, int unknown) {
        this.stage = ConfigHelper.getString(MyParams.S_LINK);
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getString(MyParams.S_HEIGHT));
        this.time = System.currentTimeMillis();
        this.dealer = dealer;
        this.driver = driver;
        this.unknown = unknown;
    }

    public void addBucket(long time, String epc, String bodyCode) {
        bucket_info.add(new Bucket(time, epc, bodyCode));
    }

    // 桶信息的内部类
    private class Bucket {

        private long bucket_time;

        private String bucket_epc;

        private String bodycode;

        private Bucket(long time, String epc, String bodycode) {
            this.bucket_time = time;
            this.bucket_epc = epc;
            this.bodycode = bodycode;
        }
    }
}
