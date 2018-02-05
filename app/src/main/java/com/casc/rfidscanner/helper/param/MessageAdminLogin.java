package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.ConfigHelper;

public class MessageAdminLogin {

    private String epc;

    private long logintime;

    private String reader_TID;

    public MessageAdminLogin(String epc) {
        this.epc = epc;
        this.logintime = System.currentTimeMillis() / 1000;
        this.reader_TID = ConfigHelper.getParam(MyParams.S_READER_ID);
    }

    public String getEpc() {
        return epc;
    }

    public long getLogintime() {
        return logintime;
    }

    public String getReader_TID() {
        return reader_TID;
    }
}
