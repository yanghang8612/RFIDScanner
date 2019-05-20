package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

public class MsgUnstack {

    @SerializedName("reader_id")
    private String readerID;

    @SerializedName("unpackage_time")
    private long time;

    private String qrcode;

    public MsgUnstack(String qrcode) {
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.time = System.currentTimeMillis();
        this.qrcode = qrcode;
    }
}
