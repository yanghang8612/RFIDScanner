package com.casc.rfidscanner.helper.param;

public class MessageQuery {

    private String bucket_TID;

    private String bodycode;

    public MessageQuery(String bucket_TID, String bodycode) {
        this.bucket_TID = bucket_TID;
        this.bodycode = bodycode;
    }

    public String getBucket_TID() {
        return bucket_TID;
    }

    public void setBucket_TID(String bucket_TID) {
        this.bucket_TID = bucket_TID;
    }

    public String getBodycode() {
        return bodycode;
    }

    public void setBodycode(String bodycode) {
        this.bodycode = bodycode;
    }
}
