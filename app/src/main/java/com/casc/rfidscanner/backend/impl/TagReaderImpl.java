package com.casc.rfidscanner.backend.impl;

import android.content.Context;

import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.ul6.USBService;
import com.casc.rfidscanner.helper.BTServiceHelper;
import com.casc.rfidscanner.ul6.impl.BluetoothServiceHandler;

/**
 *
 */
public class TagReaderImpl implements TagReader {
    private static final String TAG = TagReaderImpl.class.getSimpleName();

    private USBService usbService;

    private BTServiceHelper BTServiceHelper;
    private BluetoothServiceHandler bluetoothServiceHandler;

    @Override
    public boolean initReader() {
        return false;
    }

    @Override
    public byte[] sendCommand(byte[] cmd) {
        BTServiceHelper.write(cmd);
        return new byte[0];
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    private void setupBluetooth(Context context) {
//        bluetoothService = new INSTANT!
//        bluetoothServiceHandler = new BluetoothServiceHandler(bluetoothService);
        bluetoothServiceHandler = null;
        BTServiceHelper = new BTServiceHelper(context, bluetoothServiceHandler);
    }
}
