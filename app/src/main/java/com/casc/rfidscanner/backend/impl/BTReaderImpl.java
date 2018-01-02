package com.casc.rfidscanner.backend.impl;

import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.helper.BTServiceHelper;

/**
 *
 */

public class BTReaderImpl implements TagReader {
    private static final String TAG = BTReaderImpl.class.getSimpleName();

    private BTServiceHelper btServiceHelper;

    public BTReaderImpl(BTServiceHelper btServiceHelper) {
        this.btServiceHelper = btServiceHelper;
    }

    @Override
    public boolean initReader() {
        return false;
    }

    @Override
    public byte[] sendCommand(byte[] cmd) {
        btServiceHelper.write(cmd);
        return new byte[0];
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}
