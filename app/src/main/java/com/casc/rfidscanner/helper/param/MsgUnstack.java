package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

public class MsgUnstack {


    private String reader_TID;

    private long timestamp;

    private String bodycode;

    public MsgUnstack(String bodycode) {
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
        this.timestamp = System.currentTimeMillis();
        this.bodycode = bodycode;
    }
}
