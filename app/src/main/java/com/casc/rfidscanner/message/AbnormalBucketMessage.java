package com.casc.rfidscanner.message;

import com.casc.rfidscanner.utils.CommonUtils;

public class AbnormalBucketMessage {

    public boolean isReadNone;

    public byte[] epc;

    public AbnormalBucketMessage() {
        this.isReadNone = true;
    }

    public AbnormalBucketMessage(String epc) {
        this.isReadNone = false;
        this.epc = CommonUtils.hexToBytes(epc);
    }
}
