package com.casc.rfidscanner.backend;

import com.casc.rfidscanner.exception.NotSetTagCacheException;

public interface TagScanner {

    boolean connectToReader();

    void disconnectFromReader();

    boolean isReaderConnected();

    void setTagCache(TagCache cache);

    void startScan() throws NotSetTagCacheException;

    void stopScan();

    boolean isScanning();
}
