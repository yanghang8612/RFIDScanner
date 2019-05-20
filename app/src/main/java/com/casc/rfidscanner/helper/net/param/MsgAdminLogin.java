package com.casc.rfidscanner.helper.net.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.helper.SpHelper;
import com.google.gson.annotations.SerializedName;

public class MsgAdminLogin {

    @SerializedName("reader_id")
    private String readerID;

    @SerializedName("card_epc")
    private String cardEPC;

    @SerializedName("login_time")
    private long loginTime;


    public MsgAdminLogin(String cardEPC) {
        this.readerID = SpHelper.getString(MyParams.S_READER_ID);
        this.cardEPC = cardEPC;
        this.loginTime = System.currentTimeMillis();
    }
}
