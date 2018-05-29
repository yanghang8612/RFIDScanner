package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

public class MessageConfig {

    private String reader_TID;

    public MessageConfig() {
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
    }
}
