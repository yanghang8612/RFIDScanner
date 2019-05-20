package com.casc.rfidscanner.helper.net.param;


import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.Card;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.utils.CommonUtils;
import com.google.gson.annotations.SerializedName;

public class MsgCardReg {

    private String epc;

    private String tid;

    private String type;

    private String code;

    private String company;

    private long time;

    private double longitude;

    private double latitude;

    private double height;

    @SerializedName("expire_time")
    private long exprieTime;

    private String mark;

    public MsgCardReg(Card card) {
        this.epc = CommonUtils.bytesToHex(card.getEPC());
        this.tid = CommonUtils.bytesToHex(card.getTID());
        this.type = card.getType();
        this.code = card.getBodyCode();
        this.company = MyVars.config.getCompanySymbol();
        this.time = card.getTime();
        this.longitude = Double.valueOf(SpHelper.getString(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(SpHelper.getString(MyParams.S_LATITUDE));
        this.height = Double.valueOf(SpHelper.getString(MyParams.S_HEIGHT));
        this.exprieTime = card.getLife();
        this.mark = card.getComment();
    }
}
