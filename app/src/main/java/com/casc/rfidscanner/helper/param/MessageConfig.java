package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

/**
 * Created by Asuka on 2018/1/5.
 */

public class MessageConfig {

    private String reader_TID;

    public MessageConfig() {
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
    }

    public String getReader_TID() {
        return reader_TID;
    }

    public void setReader_TID(String reader_TID) {
        this.reader_TID = reader_TID;
    }
}
