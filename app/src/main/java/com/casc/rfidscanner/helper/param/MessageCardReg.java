package com.casc.rfidscanner.helper.param;


import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.Card;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.utils.CommonUtils;

public class MessageCardReg {

    private String card_TID;

    private String card_type;

    private String card_code;

    private String company;

    private long registertime;

    private double longitude;

    private double latitude;

    private double height;

    private String epc;

    private long datetime;

    private String mark;

    private String usable;

    public MessageCardReg(Card card) {
        this.card_TID = CommonUtils.bytesToHex(card.getTid());
        this.card_type = card.getType();
        this.card_code = card.getBodyCode();
        this.company = MyVars.config.getCompanySymbol();
        this.registertime = card.getTime() / 1000;
        this.longitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LONGITUDE));
        this.latitude = Double.valueOf(ConfigHelper.getParam(MyParams.S_LATITUDE));
        this.height = Double.valueOf(ConfigHelper.getParam(MyParams.S_HEIGHT));
        this.epc = CommonUtils.bytesToHex(card.getEpc());
        this.datetime = card.getLife();
        this.mark = card.getComment();
        this.usable = "1";
    }
}
