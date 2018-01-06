package com.casc.rfidscanner.backend.impl;

import android.content.Context;

import com.casc.rfidscanner.backend.TagReader;

/**
 *
 */
public class TagReaderImpl implements TagReader {
    private static final String TAG = TagReaderImpl.class.getSimpleName();


    @Override
    public boolean connectReader(Context context) throws Exception {
        return false;
    }

    @Override
    public boolean initReader(Context context) {
        return false;
    }

    @Override
    public void sendCommand(byte[] cmd) {
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    private void setupBluetooth(Context context) {
    }
}
