package com.casc.rfidscanner.backend.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.casc.rfidscanner.GlobalParams;
import com.casc.rfidscanner.backend.InstructionDeal;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.helper.InstructionsHelper;
import com.casc.rfidscanner.helper.SharedPreferencesHelper;
import com.casc.rfidscanner.utils.ClsUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Bluetooth
 */
public class BTReaderImpl implements TagReader {
    private static final String TAG = BTReaderImpl.class.getSimpleName();

    private static final boolean D = true;

    private InstructionDeal instructionDeal;

    // Unique UUID for this application
    //private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 串口
//    private static final UUID MY_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB"); // HID（人机交互设备）

    private static final String DEFAULT_MAC_ADDRESS = "00:13:EF:D1:90:73";

    // Member fields
    private final BluetoothAdapter mAdapter;
    private BTConnectThread mBTConnectThread;
    private BTConnectedThread mConnectedThread;
    private int mState;

    private BroadcastReceiver searchDevices = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String btReaderMAC = (String) SharedPreferencesHelper.getParam(context, GlobalParams.S_READER_MAC, DEFAULT_MAC_ADDRESS);
            if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() == BluetoothDevice.BOND_NONE) { // 未配对
                    if (device.getAddress().equals(btReaderMAC)) {
                        // 进行配对
//                        device.createBond();
                        try {
                            ClsUtils.createBond(device.getClass(), device);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                // 搜索结束
            }
        }
    };

    public BTReaderImpl(ContextWrapper context, InstructionDeal instructionDeal) {
        if (instructionDeal != null) {
            this.instructionDeal = instructionDeal;
        } else {
            throw new IllegalArgumentException(TAG + " InstructionDeal is NULL!");
        }

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;

        // 注册Receiver来获取蓝牙设备相关的结果
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);// 用BroadcastReceiver来取得搜索结果
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intent.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        context.registerReceiver(searchDevices, intent);
    }

    @Override
    public boolean connectReader(Context context) throws Exception {
        // 没有蓝牙
        if (mAdapter == null) {
            return false;
        }

        // 自动连接指定的MAC地址
        String btReaderMAC = (String) SharedPreferencesHelper.getParam(context, GlobalParams.S_READER_MAC, "");
        Log.e(TAG, "btReaderMAC = " + btReaderMAC);
        boolean isAutoConnected = autoConnection(btReaderMAC);
        if (!isAutoConnected) {
            return false;
        }

        return true;
    }

    @Override
    public boolean initReader(Context context) {
        // 工位
//        Integer btLink = (Integer) SharedPreferencesHelper.getParam(context, GlobalParams.S_LINK, 0);

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

    /**
     * 开启蓝牙搜索（更换MAC地址时调用此函数，触发配对）
     */
    public void startDiscovery() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        mAdapter.startDiscovery();
    }


    //得到配对的设备列表，清除已配对的设备
    private void removePairDevice() {
        if (mAdapter != null) {
            // 获取已配对蓝牙列表的方法
            Set<BluetoothDevice> bondedDevices = mAdapter.getBondedDevices();
            for (BluetoothDevice device : bondedDevices) {
                unpairDevice(device);
            }
        }
    }

    //反射来调用BluetoothDevice.removeBond取消设备的配对
    private void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            Log.e("mate", e.getMessage());
        }
    }

    /**
     * APP 启动时，自动连接指定 MAC 地址的蓝牙设备（本机已配对设备）
     *
     * @param address
     * @return
     */
    private boolean autoConnection(String address) {
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();// 获取本机已配对设备
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device1 : pairedDevices) {
                if (device1.getAddress().equals(address)) {
                    // Get the BluetoothDevice object
//                    BluetoothDevice device = device1;
                    // Attempt to connect to the device
                    final BluetoothDevice d = device1;
//                    new Thread(new Runnable() {
//                        @Override
//                        public void run() {
//                            connect(d);
//                        }
//                    });
                    connect(d);
                    return true;
                }
            }
        }
        return false;
    }

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
        if (mBTConnectThread != null) {
            mBTConnectThread.cancel();
            mBTConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        setState(STATE_LISTEN);
    }

    /**
     * Start the BTConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    private synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mBTConnectThread != null) {
                mBTConnectThread.cancel();
                mBTConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mBTConnectThread = new BTConnectThread(device);
        mBTConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the BTConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mBTConnectThread != null) {
            mBTConnectThread.cancel();
            mBTConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new BTConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
        instructionDeal.onConnectionStart(); //
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mBTConnectThread != null) {
            mBTConnectThread.cancel();
            mBTConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Write to the BTConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see BTConnectedThread#write(byte[])
     */
    private void write(byte[] out) {
        // Create temporary object
        BTConnectedThread r;
        // Synchronize a copy of the BTConnectedThread
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
        BTReaderImpl.this.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class BTConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public BTConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the given BluetoothDevice
            try {
//                tmp = device.createRfcommSocketToServiceRecord(MY_UUID); // SECURE
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID); // INSECURE
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mBTConnectThread");
            setName("BTConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                this.start();
                return;
            }

            // Reset the BTConnectThread because we're done
            synchronized (this) {
                mBTConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class BTConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public BTConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create BTConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN BT mConnectedThread");

            byte[] data = new byte[64];
            byte[] inBytes = new byte[64];

            // Keep listening to the InputStream while connected
            int totalCount = 0, length = 0;
            boolean startFlag = false;
            while (true) {
                try {
                    // Read from the InputStream
                    int readCount = mmInStream.read(inBytes);
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

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
