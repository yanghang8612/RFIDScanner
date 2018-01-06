package com.casc.rfidscanner.backend.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import com.casc.rfidscanner.GlobalParams;
import com.casc.rfidscanner.backend.InstructionDeal;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.helper.InstructionsHelper;
import com.casc.rfidscanner.helper.SharedPreferencesHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothService";

    // Unique UUID for this application
    //private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    //    private final Handler mHandler;
    //    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

//    private boolean isScanning; // 读写器接受扫描状态
//    private boolean isConnected; // 读写器的连接状态

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

//    private StringBuffer mOutStringBuffer; // String buffer for outgoing messages

    public BTReaderImpl(InstructionDeal instructionDeal) {
        this.instructionDeal = instructionDeal;

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
    }

    @Override
    public boolean connectReader(Context context) throws Exception {
        // 没有蓝牙
        if (mAdapter == null) {
            return false;
        }

        // 自动连接指定的MAC地址
        String btReaderMAC = (String) SharedPreferencesHelper.getParam(context, GlobalParams.S_READER_MAC, null);
        boolean isAutoConnected = autoConnection(btReaderMAC);
        if (!isAutoConnected) {
            return false;
        }
        // Initialize the buffer for outgoing messages
//        mOutStringBuffer = new StringBuffer("");
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

        return false;
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
     * APP 启动时，自动连接指定 MAC 地址的蓝牙设备（本机已配对设备）
     *
     * @param address
     * @return
     */
    public boolean autoConnection(String address) {
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();// 获取本机已配对设备
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device1 : pairedDevices) {
                if (device1.getAddress().equals(address)) {
                    // Get the BluetoothDevice object
//                    BluetoothDevice device = device1;
                    // Attempt to connect to the device
                    connect(device1);
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
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Called by the Activity onResume()
     */
    public synchronized void start() {
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

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
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
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
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
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
//        Message msg = mHandler.obtainMessage(GlobalParams.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString(GlobalParams.DEVICE_NAME, device.getName());
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
        instructionDeal.onConnectionStart(); //
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
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

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
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
        BTReaderImpl.this.start();
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
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
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

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

            // Reset the ConnectThread because we're done
            synchronized (this) {
                mConnectThread = null;
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
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
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
            Log.i(TAG, "BEGIN mConnectedThread");

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
