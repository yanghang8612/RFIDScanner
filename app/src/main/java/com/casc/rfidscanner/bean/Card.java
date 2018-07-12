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
                this.life = System.currentTimeMillis() / 1000 + 7 * 24 * 60 * 60;
                break;
            case "2周":
                this.life = System.currentTimeMillis() / 1000 + 2 * 7 * 24 * 60 * 60;
                break;
            case "3周":
                this.life = System.currentTimeMillis() / 1000 + 3 * 7 * 24 * 60 * 60;
                break;
            case "1个月":
                this.life = System.currentTimeMillis() / 1000 + 30 * 24 * 60 * 60;
                break;
            case "6个月":
                this.life = System.currentTimeMillis() / 1000 + 6 * 30 * 24 * 60 * 60;
                break;
            case "12个月":
                this.life = System.currentTimeMillis() / 1000 + 12 * 30 * 24 * 60 * 60;
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
                this.pc = CommonUtils.hexToBytes(MyParams.DELIVERY_PC_CONTENT);
                this.epc = new byte[MyParams.EPC_DELIVERY_CARD_LENGTH];
                this.type = String.valueOf(MyParams.EPCType.CARD_DELIVERY.getCode());
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, MyParams.EPC_HEADER_LENGTH);
                epc[MyParams.EPC_TYPE_INDEX] = MyParams.EPCType.CARD_DELIVERY.getCode();
                epc[MyParams.EPC_TYPE_INDEX + 1] = (byte) (Integer.valueOf(bodyCode.substring(4)) >> 8);
                epc[MyParams.EPC_TYPE_INDEX + 2] = (byte) (Integer.valueOf(bodyCode.substring(4)) & 0xFF);
                break;
            case "运维专用卡":
                this.pc = CommonUtils.hexToBytes(MyParams.ADMIN_PC_CONTENT);
                this.epc = new byte[MyParams.EPC_ADMIN_CARD_LENGTH];
                this.type = String.valueOf(MyParams.EPCType.CARD_ADMIN.getCode());
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, MyParams.EPC_HEADER_LENGTH);
                epc[MyParams.EPC_TYPE_INDEX] = MyParams.EPCType.CARD_ADMIN.getCode();
                epc[MyParams.EPC_TYPE_INDEX + 1] = (byte) (Integer.valueOf(bodyCode.substring(4)) >> 8);
                epc[MyParams.EPC_TYPE_INDEX + 2] = (byte) (Integer.valueOf(bodyCode.substring(4)) & 0xFF);
                System.arraycopy(BigInteger.valueOf(this.life).toByteArray(), 0, epc, MyParams.EPC_TYPE_INDEX + 3, 4);
                break;
            case "回流专用卡":
                this.pc = CommonUtils.hexToBytes(MyParams.REFLUX_PC_CONTENT);
                this.epc = new byte[MyParams.EPC_REFLUX_CARD_LENGTH];
                this.type = String.valueOf(MyParams.EPCType.CARD_REFLUX.getCode());
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, MyParams.EPC_HEADER_LENGTH);
                epc[MyParams.EPC_TYPE_INDEX] = MyParams.EPCType.CARD_REFLUX.getCode();
                epc[MyParams.EPC_TYPE_INDEX + 1] = (byte) (Integer.valueOf(bodyCode.substring(4)) >> 8);
                epc[MyParams.EPC_TYPE_INDEX + 2] = (byte) (Integer.valueOf(bodyCode.substring(4)) & 0xFF);
                break;
        }
    }

    public byte[] getPc() {
        return pc;
    }

    public byte[] getEpc() {
        return epc;
    }

    public byte[] getTid() {
        return tid;
    }

    public String getType() {
        return type;
    }

    public void setTid(byte[] tid) {
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
