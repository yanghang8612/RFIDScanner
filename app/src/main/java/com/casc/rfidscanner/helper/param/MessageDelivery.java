package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class MessageDelivery {

    private String stage;

    private String reader_TID;

    private double longitude;

    private double latitude;

    private double height;

    private long time;

    private String formnumber;

    private char accordance;

    private String dealer;

    private String driver;

    // 成品出库的桶身码相关扫描信息
    private List<Bucket> bucket_info = new ArrayList<>();

    public MessageDelivery(String formnumber, char accordance, String dealer, String driver) {
        this.stage = "06";
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.longitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getString(MyParams.S_HEIGHT));
        this.time = System.currentTimeMillis() - (MyParams.DELAY * 5);
        this.formnumber = formnumber;
        this.accordance = accordance;
        this.dealer = dealer;
        this.driver = driver;
    }

    public void addBucket(long time, byte[] epc, String flag) {
        bucket_info.add(new Bucket(time, epc, flag.equals("3") || flag.equals("4") ? "0" : flag));
    }

    // 桶信息的内部类
    private class Bucket {

        private long bucket_time;

        private String bucket_epc;

        private String flag;

        private Bucket(long time, byte[] epc, String flag) {
            this.bucket_time = time - (MyParams.DELAY * 5);
            this.bucket_epc = CommonUtils.bytesToHex(epc);
            this.flag = flag;
        }
    }
}
