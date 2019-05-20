package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.utils.CommonUtils;

import java.math.BigInteger;

public class Card {

    private byte[] pc;

    private byte[] epc;

    private byte[] tid;

    private long time;

    private long life;

    private String type;

    private String bodyCode;

    private String validity;

    private String comment;

    public Card(String type, String bodyCode, String validity) {
        switch (validity) {
            case "1周":
                this.life = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000;
                break;
            case "2周":
                this.life = System.currentTimeMillis() + 2L * 7 * 24 * 60 * 60 * 1000;
                break;
            case "3周":
                this.life = System.currentTimeMillis() + 3L * 7 * 24 * 60 * 60 * 1000;
                break;
            case "1个月":
                this.life = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000;
                break;
            case "6个月":
                this.life = System.currentTimeMillis() + 6L * 30 * 24 * 60 * 60 * 1000;
                break;
            case "12个月":
                this.life = System.currentTimeMillis() + 12L * 30 * 24 * 60 * 60 * 1000;
                break;
            case "永久":
                this.life = 0;
                break;
        }
        this.time = System.currentTimeMillis();
        this.bodyCode = bodyCode;
        this.validity = validity;
        this.comment = type;
        switch (type) {
            case "出库专用卡":
                this.pc = CommonUtils.generatePC(MyParams.EPC_DELIVERY_CARD_LENGTH);
                this.epc = new byte[MyParams.EPC_DELIVERY_CARD_LENGTH];
                this.type = String.valueOf(EPCType.CARD_DELIVERY.getCode());
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, MyParams.EPC_HEADER_LENGTH);
                epc[MyParams.EPC_TYPE_INDEX] = EPCType.CARD_DELIVERY.getCode();
                epc[MyParams.EPC_TYPE_INDEX + 1] = (byte) (Integer.valueOf(bodyCode.substring(4)) >> 8);
                epc[MyParams.EPC_TYPE_INDEX + 2] = (byte) (Integer.valueOf(bodyCode.substring(4)) & 0xFF);
                break;
            case "运维专用卡":
                this.pc = CommonUtils.generatePC(MyParams.EPC_ADMIN_CARD_LENGTH);
                this.epc = new byte[MyParams.EPC_ADMIN_CARD_LENGTH];
                this.type = String.valueOf(EPCType.CARD_ADMIN.getCode());
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, MyParams.EPC_HEADER_LENGTH);
                epc[MyParams.EPC_TYPE_INDEX] = EPCType.CARD_ADMIN.getCode();
                epc[MyParams.EPC_TYPE_INDEX + 1] = (byte) (Integer.valueOf(bodyCode.substring(4)) >> 8);
                epc[MyParams.EPC_TYPE_INDEX + 2] = (byte) (Integer.valueOf(bodyCode.substring(4)) & 0xFF);
                System.arraycopy(BigInteger.valueOf(this.life).toByteArray(), 0, epc, MyParams.EPC_TYPE_INDEX + 3, 4);
                break;
            case "回流专用卡":
                this.pc = CommonUtils.generatePC(MyParams.EPC_REFLUX_CARD_LENGTH);
                this.epc = new byte[MyParams.EPC_REFLUX_CARD_LENGTH];
                this.type = String.valueOf(EPCType.CARD_REFLUX.getCode());
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, MyParams.EPC_HEADER_LENGTH);
                epc[MyParams.EPC_TYPE_INDEX] = EPCType.CARD_REFLUX.getCode();
                epc[MyParams.EPC_TYPE_INDEX + 1] = (byte) (Integer.valueOf(bodyCode.substring(4)) >> 8);
                epc[MyParams.EPC_TYPE_INDEX + 2] = (byte) (Integer.valueOf(bodyCode.substring(4)) & 0xFF);
                break;
        }
    }

    public byte[] getPC() {
        return pc;
    }

    public byte[] getEPC() {
        return epc;
    }

    public byte[] getTID() {
        return tid;
    }

    public String getType() {
        return type;
    }

    public void setTID(byte[] tid) {
        this.tid = tid;
    }

    public long getTime() {
        return time;
    }

    public long getLife() {
        return life;
    }

    public String getBodyCode() {
        return bodyCode;
    }

    public String getValidity() {
        return validity;
    }

    public String getComment() {
        return comment;
    }

    public void setSpecial() {
        epc[MyParams.EPC_TYPE_INDEX + 3] = (byte) 0x01;
    }
}
