package com.casc.rfidscanner.message;

public class AbnormalBucketMessage {

    public boolean isReadNone;

    public String epc;

    public AbnormalBucketMessage() {
        this.isReadNone = true;
    }

    public AbnormalBucketMessage(String epc) {
        this.isReadNone = false;
        this.epc = epc;
    }
}
