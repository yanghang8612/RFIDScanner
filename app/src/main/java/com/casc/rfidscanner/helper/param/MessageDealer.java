package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageDealer {

    private String stage;

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    private long time;

    private String counterparty;

    private String driver;

    // 成品出库的桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageDealer(String stage) {
        this.stage = stage;
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getParam(MyParams.S_HEIGHT));
        this.time = System.currentTimeMillis() / 1000 - (MyParams.DELAY * (14 - Integer.valueOf(stage)));
        this.counterparty = "";
        this.driver = "";
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

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getCounterparty() {
        return counterparty;
    }

    public void setCounterparty(String counterparty) {
        this.counterparty = counterparty;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public List<Bucket> getBucket_info() {
        return bucket_info;
    }

    public void setBucket_info(List<Bucket> bucket_info) {
        this.bucket_info = bucket_info;
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
            this.bucket_time = time - (MyParams.DELAY * (14 - Integer.valueOf(stage)));
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
