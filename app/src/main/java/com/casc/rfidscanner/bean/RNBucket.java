package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.utils.CommonUtils;

public class RNBucket {

    private long time;

    private String epc;

    private String bodyCode;

    private boolean isHighlight;

    public RNBucket(byte[] epc) {
        this.time = System.currentTimeMillis();
        this.epc = CommonUtils.bytesToHex(epc);
        int code = 0;
        code += ((epc[13] & 0xFF) << 16);
        code += ((epc[14] & 0xFF) << 8);
        code += (epc[15] & 0xFF);
        this.bodyCode = "HT" + String.format("%06d", code);
        this.isHighlight = true;
    }

    public long getTime() {
        return time;
    }

    public String getEpc() {
        return epc;
    }

    public String getBodyCode() {
        return bodyCode;
    }

    public boolean isHighlight() {
        return isHighlight;
    }

    public void setHighlight(boolean highlight) {
        isHighlight = highlight;
    }
}
