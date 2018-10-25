package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

public class MsgReaderTID {

    private String reader_TID;

    public MsgReaderTID() {
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
    }
}
