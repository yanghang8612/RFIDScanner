package com.casc.rfidscanner.backend.impl;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.casc.rfidscanner.backend.InstructionDeal;
import com.casc.rfidscanner.backend.TagReader;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Map;

/**
 * USB
 */
public class USBReaderImpl extends Service implements TagReader {
    private static final String TAG = USBReaderImpl.class.getSimpleName();

    private InstructionDeal instructionDeal;

    private static final String ACTION_USB_PERMISSION = "INTENT.USB_PERMISSION";
    private static final Integer BAUD_RATE = 115200;

    private boolean isScanning; // 扫描状态
    private boolean isConnected; // 连接状态

    private UsbManager usbManager;
    private UsbDevice usbReader;
    private UsbSerialPort usbReaderPort;

    public USBReaderImpl(InstructionDeal instructionDeal) {
        this.instructionDeal = instructionDeal;

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE); // extends Service
        isScanning = false;
        isConnected = false;
    }

    @Override
    public boolean connectReader(Context context) throws Exception {
        Map<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        if (!deviceMap.isEmpty()) {
            for (UsbDevice device : deviceMap.values()) {
                usbReader = device;
                Log.i(TAG, device.getDeviceName() + " with vid = " + device.getVendorId() + " --- pid = " + device.getProductId());
            }
            usbReaderPort = new Cp21xxSerialDriver(usbReader).getPorts().get(0);
            // 若建立失败则抛出异常，交由上层逻辑处理（视异常而请求USB权限等）
            usbReaderPort.open(usbManager.openDevice(usbReader));
            usbReaderPort.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            isConnected = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean initReader(Context context) throws Exception {
        return false;
    }

    @Override
    public void sendCommand(byte[] cmd) {

    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
