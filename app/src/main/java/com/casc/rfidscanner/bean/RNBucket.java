package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.utils.CommonUtils;

public class RNBucket {

    private long time;

    private String epc;

    private String bodyCode = "";

    private boolean isHighlight = true;

    public RNBucket(byte[] epc) {
        this.time = System.currentTimeMillis();
        this.epc = CommonUtils.bytesToHex(epc);
        for (int i = 0; i < MyParams.BODY_CODE_HEADER_LENGTH; i++) {
            this.bodyCode = String.format("%s%s", this.bodyCode, (char) epc[1 + i]);
        }
        for (int i = 0; i < MyParams.BODY_CODE_CONTENT_LENGTH; i++) {
            this.bodyCode = String.format("%s%s", this.bodyCode, (char) epc[7 + i]);
        }
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
