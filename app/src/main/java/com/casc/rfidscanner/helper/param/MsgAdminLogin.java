package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

public class MsgAdminLogin {

    private String epc;

    private long logintime;

    private String reader_TID;

    public MsgAdminLogin(String epc) {
        this.epc = epc;
        this.logintime = System.currentTimeMillis();
        this.reader_TID = ConfigHelper.getString(MyParams.S_READER_ID);
    }
}
