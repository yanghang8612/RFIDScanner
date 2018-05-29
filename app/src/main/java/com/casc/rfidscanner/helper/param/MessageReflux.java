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

    // 成品出库的桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageReflux(String dealer, String driver) {
        this.stage = ConfigHelper.getParam(MyParams.S_LINK);
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getParam(MyParams.S_HEIGHT));
        this.time = System.currentTimeMillis() / 1000;
        this.dealer = dealer;
        this.driver = driver;
    }

    public void addBucket(long time, String epc) {
        bucket_info.add(new Bucket(time, epc));
    }

    // 桶信息的内部类
    private class Bucket {

        private long bucket_time;

        private String bucket_epc;

        private Bucket(long time, String epc) {
            this.bucket_epc = epc;
            this.bucket_time = time;
        }
    }
}
