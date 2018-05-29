package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageScrap {

    private String stage;

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    // R1、R3、R4消息中包含的所有桶RFID及桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageScrap() {
        this.stage = "01";
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getParam(MyParams.S_HEIGHT));
    }

    public void addBucket(String tid, String epc, int code) {
        bucket_info.add(new Bucket(tid, epc, code));
    }

    // 桶信息的内部类
    private class Bucket {

        private String bucket_TID;

        private long bucket_time;

        private String bucket_epc;

        private int disablecode;

        private Bucket(String tid, String epc, int code) {
            this.bucket_TID = tid;
            this.bucket_time = System.currentTimeMillis() / 1000 - (MyParams.DELAY * (10 - Integer.valueOf(stage)));
            this.bucket_epc = epc;
            this.disablecode = code;
        }
    }
}
