package com.casc.rfidscanner.message;

public class TagUploadedMessage {

    public final boolean isFromDB;

    public TagUploadedMessage(boolean isFromDB) {
        this.isFromDB = isFromDB;
    }
}
