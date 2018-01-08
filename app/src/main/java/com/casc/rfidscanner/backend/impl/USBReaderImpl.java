package com.casc.rfidscanner.backend.impl;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.casc.rfidscanner.GlobalParams;
import com.casc.rfidscanner.backend.InstructionDeal;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.exception.NotSetTagCacheException;
import com.casc.rfidscanner.helper.InstructionsHelper;
import com.casc.rfidscanner.helper.SharedPreferencesHelper;
import com.casc.rfidscanner.service.ScanService;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * USB
 */
public class USBReaderImpl implements TagReader {
    private static final String TAG = USBReaderImpl.class.getSimpleName();

    private static final boolean D = true;

    public static final String ACTION_USB_PERMISSION = "INTENT.USB_PERMISSION";
    private Context context;

    private InstructionDeal instructionDeal;

    private static final Integer BAUD_RATE = 115200;
    private static final Integer WRITE_TIMEOUT = 1000; // ms

    private static final Integer DEFAULT_VID = 0x10c4;
    private static final Integer DEFAULT_PID = 0xea60;

    private UsbManager usbManager;
    private USBConnectThread mConnectThread;
    private USBConnectedThread mConnectedThread;
    private int mState;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    try {
                        connectReader(context);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                try {
                    connectReader(context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                instructionDeal.onConnectionLost(); //
            }
        }
    };

    public USBReaderImpl(ContextWrapper context, InstructionDeal instructionDeal) {
        if (instructionDeal != null) {
            this.instructionDeal = instructionDeal;
        } else {
            throw new IllegalArgumentException(TAG + " InstructionDeal is NULL!");
        }
        this.context = context;

        // 获取USB设备
//        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE); // extends Service
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE); // extends Service
        mState = STATE_NONE;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public boolean connectReader(Context context) throws Exception {
        // 获取到设备列表
        Map<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        if (deviceMap != null && !deviceMap.isEmpty()) {
            for (UsbDevice device : deviceMap.values()) {
                if (device.getVendorId() == DEFAULT_VID && device.getProductId() == DEFAULT_PID) {
                    Log.i(TAG, device.getDeviceName() + " with vid = " + device.getVendorId() + " AND pid = " + device.getProductId());
                    connect(device);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean initReader(Context context) throws Exception {
        // 设置发射功率
        Integer btReaderPower = (Integer) SharedPreferencesHelper.getParam(context, GlobalParams.S_READER_POWER, 15);
        byte[] transmitPower = InstructionsHelper.setTransmitPower(btReaderPower);
        sendCommand(transmitPower);

        // 设置Q值
        Integer btReaderQvalue = (Integer) SharedPreferencesHelper.getParam(context, GlobalParams.S_READER_QVALUE, 1);
        byte[] queryParameter = InstructionsHelper.setQueryParameter((byte) 0x00, (byte) 0x00, (byte) 0x00, btReaderQvalue);
        sendCommand(queryParameter);

        return true;
    }

    @Override
    public void sendCommand(byte[] cmd) {
        write(cmd);
    }

    @Override
    public boolean isConnected() {
        if (mState == STATE_CONNECTED) return true;
        return false;
    }

//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }


    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;
    }

    /**
     * Return the current connection state.
     */
    private synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Called by the Activity onResume()
     */
    private synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    private synchronized void connect(UsbDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new USBConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    private synchronized void connected(UsbSerialPort socket, UsbDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new USBConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
        instructionDeal.onConnectionStart(); //
    }

    /**
     * Stop all threads
     */
    private synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    private void write(byte[] out) {
        // Create temporary object
        USBConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);
    }

    /**
     * Indicate that the connection was lost.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);
        instructionDeal.onConnectionLost(); //

        // Start the service over to restart listening mode
        USBReaderImpl.this.start();
    }

    private class USBConnectThread extends Thread {
        private UsbDevice mmDevice;
        private UsbSerialPort mmSerialPort;

        public USBConnectThread(UsbDevice device) {
            mmDevice = device;
            Cp21xxSerialDriver cp21xxSerialDriver = new Cp21xxSerialDriver(mmDevice);
            mmSerialPort = cp21xxSerialDriver.getPorts().get(0);
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("USBConnectThread");

            // Make a connection
            try {
                mmSerialPort.open(usbManager.openDevice(mmDevice));
                mmSerialPort.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
            }catch (NullPointerException en) {
                en.printStackTrace();
                usbManager.requestPermission(mmDevice, PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0));
            }
            catch (IOException e) {
                connectionFailed();
                try {
                    mmSerialPort.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() UsbSerialPort during connection failure", e2);
                }
                this.start();
                return;
            }

            // Reset the BTConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSerialPort, mmDevice);
        }

        public void cancel() {
            try {
                mmSerialPort.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect UsbSerialPort failed", e);
            }
        }
    }

    /**
     *
     */
    private class USBConnectedThread extends Thread {
        private UsbSerialPort usbReaderPort;

        public USBConnectedThread(UsbSerialPort usbReaderPort) {
            this.usbReaderPort = usbReaderPort;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");

            byte[] data = new byte[64];
            byte[] inBytes = new byte[64];

            // Keep listening to the InputStream while connected
            int totalCount = 0, length = 0;
            boolean startFlag = false;
            while (true) {
                try {
                    // Read from the InputStream
                    int readCount = usbReaderPort.read(inBytes, 1000);
                    for (int i = 0; i < readCount; i++) {
                        if (!startFlag && inBytes[i] == InstructionsHelper.INS_HEADER) { // (byte) 0xBB
                            startFlag = true;
                        }
                        if (startFlag) {
                            data[totalCount++] = inBytes[i];
                            if (totalCount == 5) {
                                length = data[4];
                            }
                            if (totalCount == length + 7) {
                                //Log.i(TAG, CommonUtils.bytesToHex(data, totalCount));
                                if (totalCount > 20) {
                                    instructionDeal.callback(Arrays.copyOf(data, data.length)); // 回调函数
                                }
                                totalCount = length = 0;
                                startFlag = false;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                usbReaderPort.write(buffer, WRITE_TIMEOUT);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                usbReaderPort.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
