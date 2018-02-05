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
                this.type = "1";
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, 5);
                epc[5] = (byte) 0x01;
                epc[6] = (byte) (int) Integer.valueOf(bodyCode.substring(3));
                break;
            case "运维专用卡":
                this.pc = CommonUtils.hexToBytes(MyParams.ADMIN_PC_CONTENT);
                this.epc = new byte[MyParams.EPC_ADMIN_CARD_LENGTH];
                this.type = "2";
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, 5);
                epc[5] = (byte) 0x02;
                epc[7] = (byte) (int) Integer.valueOf(bodyCode.substring(3));
                System.arraycopy(BigInteger.valueOf(this.life).toByteArray(), 0, epc, 8, 4);
                break;
            case "回流专用卡":
                this.pc = CommonUtils.hexToBytes(MyParams.REFLUX_PC_CONTENT);
                this.epc = new byte[MyParams.EPC_REFLUX_CARD_LENGTH];
                this.type = "3";
                System.arraycopy(CommonUtils.generateEPCHeader(), 0, this.epc, 0, 5);
                epc[5] = (byte) 0x03;
                epc[7] = (byte) (int) Integer.valueOf(bodyCode.substring(3));
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
}
