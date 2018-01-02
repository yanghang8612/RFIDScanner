package com.casc.rfidscanner.backend.impl;

import com.casc.rfidscanner.backend.TagReader;

/**
 *
 */

public class USBReaderImpl implements TagReader {
    private static final String TAG = USBReaderImpl.class.getSimpleName();

    @Override
    public boolean initReader() {
        return false;
    }

    @Override
    public byte[] sendCommand(byte[] cmd) {
        return new byte[0];
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
