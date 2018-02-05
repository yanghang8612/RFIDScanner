package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageRegister {

    private String stage = "00";

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    // 桶规格信息
    private String bucketspec;

    // 桶类型信息
    private String buckettype;

    // 水品牌信息
    private String waterbrand;

    // 水规格信息
    private String waterspec;

    // 桶生产方信息
    private String bucketproducer;

    // 桶所有方信息
    private String bucketowner;

    // 桶合法使用方信息
    private String bucketuser;

    // R0注册消息中包含的所有桶RFID及桶身码相关扫描信息
    private List<BucketInfo> bucket_info = new ArrayList<>();

    public MessageRegister() {
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getParam(MyParams.S_HEIGHT));
    }

    public String getStage() {
        return stage;
    }

    public String getReader_TID() {
        return reader_TID;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getHeight() {
        return height;
    }

    public String getBucketspec() {
        return bucketspec;
    }

    public void setBucketspec(String bucketspec) {
        this.bucketspec = bucketspec;
    }

    public String getBuckettype() {
        return buckettype;
    }

    public void setBuckettype(String buckettype) {
        this.buckettype = buckettype;
    }

    public String getWaterbrand() {
        return waterbrand;
    }

    public void setWaterbrand(String waterbrand) {
        this.waterbrand = waterbrand;
    }

    public String getWaterspec() {
        return waterspec;
    }

    public void setWaterspec(String waterspec) {
        this.waterspec = waterspec;
    }

    public String getBucketproducer() {
        return bucketproducer;
    }

    public void setBucketproducer(String bucketproducer) {
        this.bucketproducer = bucketproducer;
    }

    public String getBucketowner() {
        return bucketowner;
    }

    public void setBucketowner(String bucketowner) {
        this.bucketowner = bucketowner;
    }

    public String getBucketuser() {
        return bucketuser;
    }

    public void setBucketuser(String bucketuser) {
        this.bucketuser = bucketuser;
    }

    public List<BucketInfo> getBucket_info() {
        return bucket_info;
    }

    public void addBucketInfo(String tid, String epc, String code) {
        bucket_info.add(new BucketInfo(tid, epc, code));
    }

    // 桶信息的内部类
    private class BucketInfo {

        private String bucket_TID;

        private long bucket_time;

        private String bucket_epc;

        private String bodycode;

        private BucketInfo(String tid, String epc, String code) {
            this.bucket_TID = tid;
            this.bucket_time = System.currentTimeMillis() / 1000;
            this.bucket_epc = epc;
            this.bodycode = code;
        }

        public String getBucket_TID() {
            return bucket_TID;
        }

        public void setBucket_TID(String bucket_TID) {
            this.bucket_TID = bucket_TID;
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

        public String getBodycode() {
            return bodycode;
        }

        public void setBodycode(String bodycode) {
            this.bodycode = bodycode;
        }
    }
}
