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

    // R1、R3、R4消息中包含的所有桶RFID及桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageCommon() {
        this.stage = ConfigHelper.getParam(MyParams.S_LINK);
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getParam(MyParams.S_HEIGHT));
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getReader_TID() {
        return reader_TID;
    }

    public void setReader_TID(String reader_TID) {
        this.reader_TID = reader_TID;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public List<Bucket> getBucket_info() {
        return bucket_info;
    }

    public void setBucket_info(List<Bucket> bucket_info) {
        this.bucket_info = bucket_info;
    }

    public void addBucket(String tid, String epc) {
        bucket_info.add(new Bucket(tid, epc));
    }

    // 桶信息的内部类
    private class Bucket {

        private String bucket_TID;

        private long bucket_time;

        private String bucket_epc;

        private Bucket(String tid, String epc) {
            this.bucket_TID = tid;
            this.bucket_time = System.currentTimeMillis() / 1000 - (MyParams.DELAY * (10 - Integer.valueOf(stage)));
            this.bucket_epc = epc;
        }

        public long getBucket_time() {
            return bucket_time;
        }

        public void setBucket_time(long bucket_time) {
            this.bucket_time = bucket_time;
        }

        public String getBucket_epc() {
            return bucket_epc;
        }

        public void setBucket_epc(String bucket_epc) {
            this.bucket_epc = bucket_epc;
        }
    }
}
