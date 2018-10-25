package com.casc.rfidscanner.helper.param;

public class MsgChkBodyCodeAndTID {

    private String bucket_TID;

    private String bodycode;

    public MsgChkBodyCodeAndTID(String bucket_TID, String bodycode) {
        this.bucket_TID = bucket_TID;
        this.bodycode = bodycode;
    }
}
