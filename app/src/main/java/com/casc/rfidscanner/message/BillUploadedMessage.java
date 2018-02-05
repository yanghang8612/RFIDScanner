package com.casc.rfidscanner.message;

public class BillUploadedMessage {

    public final boolean isFromDB;

    public BillUploadedMessage(boolean isFromDB) {
        this.isFromDB = isFromDB;
    }
}
