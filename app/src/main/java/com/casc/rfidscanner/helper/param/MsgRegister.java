package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MsgRegister {

    private String stage = "00";

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    private String productname;

    // R0注册消息中包含的所有桶RFID及桶身码相关扫描信息
    private List<BucketInfo> bucket_info = new ArrayList<>();

    public MsgRegister(String productname) {
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getString(MyParams.S_HEIGHT));
        this.productname = productname;
    }

    public void addBucket(String tid, String epc, String code) {
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
            this.bucket_time = System.currentTimeMillis() - (MyParams.DELAY * 8);
            this.bucket_epc = epc;
            this.bodycode = code;
        }
    }
}
