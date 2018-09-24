package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageCommon {

    private String stage;

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    // R3、R4消息中包含的所有桶RFID及桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageCommon() {
        this.stage = ConfigHelper.getString(MyParams.S_LINK);
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getString(MyParams.S_HEIGHT));
    }

    public void addBucket(String tid, String epc) {
        bucket_info.add(new Bucket(tid, epc));
    }

    public void addBucket(String epc) {
        bucket_info.add(new Bucket("", epc));
    }

    // 桶信息的内部类
    private class Bucket {

        private String bucket_TID;

        private long bucket_time;

        private String bucket_epc;

        private Bucket(String tid, String epc) {
            this.bucket_TID = tid;
            this.bucket_time = System.currentTimeMillis() - (MyParams.DELAY * (10 - Integer.valueOf(stage)));
            this.bucket_epc = epc;
        }
    }
}
