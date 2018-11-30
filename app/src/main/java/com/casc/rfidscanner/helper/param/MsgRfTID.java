package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

public class MsgRfTID {

    private String rftid;

    public MsgRfTID() {
        this.rftid = ConfigHelper.getString(MyParams.S_READER_ID);
    }
}
