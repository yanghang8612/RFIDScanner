package com.casc.rfidscanner.message;

public class TagCountChangedMessage {

    public int scannedCount;

    public int uploadedCount;

    public int storedCount;

    public TagCountChangedMessage(int scannedCount, int uploadedCount, int storedCount) {
        this.scannedCount = scannedCount;
        this.uploadedCount = uploadedCount;
        this.storedCount = storedCount;
    }
}
