package com.casc.rfidscanner.backend.impl;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.util.Log;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.utils.CommonUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TagReader的Bluetooth实现
 */
public class BLEReaderImpl implements TagReader {

    private static final String TAG = BLEReaderImpl.class.getSimpleName();

    // 返回帧的处理回调函数
    private InstructionHandler mInstructionHandler;

    // 循环发现Timer
    private CountDownTimer mDiscoveryTimer;

    // 读写器的连接状态
    private int mState = STATE_NONE;

    // 读写器的初始化状态
    private volatile boolean mIsPowerSet, mIsQValueSet;

    // 读写器的工作状态
    private volatile boolean mIsRunning;

    // 读写器指令帧的队列（写队列）
    private ConcurrentLinkedQueue<byte[]> mWriteQueue = new ConcurrentLinkedQueue<>();

    // Unique UUID for this application
    //private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    //private static final UUID MY_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805F9B34FB"); // HID（人机交互设备）
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 串口

    // 读写器蓝牙相关字段
    private BluetoothAdapter mBLEAdapter;
    private BluetoothDevice mBLEDevice;
    private BluetoothSocket mBLESocket;
    private InputStream mInStream; // 蓝牙输入流
    private OutputStream mOutStream; // 蓝牙输出流

    public BLEReaderImpl(Context context) {
        this.mDiscoveryTimer = new DiscoveryTimer(10 * 1500, 1000);
        this.mBLEAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBLEAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBtIntent);
        }
        // 注册蓝牙广播监听器
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); // 发现设备
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    //Log.i(TAG, "Found " + scanDevice.getAddress());
                    String btReaderMAC = ConfigHelper.getParam(MyParams.S_READER_MAC);
                    if (mIsRunning && mState == STATE_NONE && scanDevice.getAddress().equals(btReaderMAC)) {
                        MyVars.executor.execute(new BLEConnectTask());
                    }
                }
            }
        };
        context.registerReceiver(mReceiver, filter);
        MyVars.executor.execute(new BLEWriteTask());
        MyVars.executor.execute(new BLEReadTask());
    }

    /**
     * 失去连接的处理
     */
    private synchronized void lostConnection() {
        if (mState == STATE_CONNECTED) {
            mState = STATE_NONE;
            mBLEDevice = null;
            try {
                mBLESocket.close();
            } catch (Exception e) {
                Log.i(TAG, "Close bluetooth socket error");
            }
            mBLESocket = null;
            mInStream = null;
            mOutStream = null;
//            MyVars.status.setReaderStatus(false);
//            if (mIsRunning)
//                EventBus.getDefault().post(MyVars.status);
            startDiscovery();
        }
    }

    @Override
    public void initReader() {
        mIsPowerSet = mIsQValueSet = false;
    }

    @Override
    public void setHandler(InstructionHandler handler) {
        this.mInstructionHandler = handler;
    }

    @Override
    public void sendCommand(byte[] cmd) {
        sendCommand(cmd, 1);
    }

    @Override
    public void sendCommand(byte[] cmd, int times) {
        if (!mIsRunning && mState == STATE_NONE) return;
        while (times-- != 0) mWriteQueue.add(cmd);
    }

    @Override
    public boolean isConnected() {
        return mState == STATE_CONNECTED;
    }

    @Override
    public void start() {
        mIsRunning = true;
        startDiscovery();
    }

    @Override
    public void resume() {
        mIsRunning = true;
        if (mBLESocket != null && !mBLESocket.isConnected() ||
                mBLEDevice != null && !ConfigHelper.getParam(MyParams.S_READER_MAC).equals(mBLEDevice.getAddress())) {
            if (mState == STATE_CONNECTED) {
                lostConnection();
            } else {
                startDiscovery();
            }
        }
    }

    @Override
    public void pause() {
        mIsRunning = false;
        if (mState == STATE_CONNECTING) {
            mDiscoveryTimer.cancel();
            mBLEAdapter.cancelDiscovery();
            try {
                mBLESocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        mIsRunning = false;
        lostConnection();
    }

    public BluetoothDevice getConnectedDevice() {
        return mBLEDevice;
    }

    /**
     * Bluetooth连接线程
     */
    private class BLEConnectTask implements Runnable {

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            Log.i(TAG, "Begin BLEConnectTask start at " + System.currentTimeMillis());
            mBLEAdapter.cancelDiscovery();
            mState = STATE_CONNECTING;
            try {
                if (mBLESocket == null) {
                    String btReaderMAC = ConfigHelper.getParam(MyParams.S_READER_MAC);
                    Set<BluetoothDevice> pairedDevices = mBLEAdapter.getBondedDevices();// 获取本机已配对设备
                    if (pairedDevices != null && pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            if (device.getAddress().equals(btReaderMAC)) {
                                mBLEDevice = device;
                                mBLESocket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                            }
                        }
                    }
                }
                mBLESocket.connect();
                mInStream = mBLESocket.getInputStream();
                mOutStream = mBLESocket.getOutputStream();
                mState = STATE_CONNECTED;
//                if (mIsRunning)
//                    EventBus.getDefault().post(MyVars.status.setReaderStatus(true));
                Log.i(TAG, "BLEConnectTask cost " + (System.currentTimeMillis() - startTime));
            } catch (IOException e) {
                Log.i(TAG, "Error when connect to bluetooth reader");
                e.printStackTrace();
                mState = STATE_NONE;
                startDiscovery();
            }
        }
    }

    private void startDiscovery() {
        if (mIsRunning && mState == STATE_NONE) {
            Log.i(TAG, "BLE start discovery");
            if (mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();
            mBLEAdapter.startDiscovery();
            mDiscoveryTimer.cancel();
            mDiscoveryTimer.start();
        }
    }

    private class DiscoveryTimer extends CountDownTimer {

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public DiscoveryTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            Log.i(TAG, "BLE discovery finish");
            mBLEAdapter.cancelDiscovery();
            startDiscovery();
        }
    }

    /**
     * Bluetooth写数据线程，该线程一直工作在后台，即使没有读写器通过USB连接
     */
    private class BLEWriteTask implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "Begin BLEWriteTask");

            // 循环从写队列中取指令帧尝试下发给读写器，若队列为空或读写器尚未连接则休眠指定时间后重试
            while (true) {
                try {
                    Thread.sleep(LinkType.getSendInterval());
                    if (!mIsRunning || mState == STATE_NONE || mOutStream == null) continue;
                    byte[] ins;
                    if (!mIsPowerSet) // 设置发射功率
                        ins = InsHelper.setTransmitPower(LinkType.getPower());
                    else if (!mIsQValueSet) // 设置Q值
                        ins = InsHelper.setQueryParameter(
                                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                Integer.valueOf(ConfigHelper.getParam(LinkType.getType().qValue)));
                    else if (mWriteQueue.isEmpty()) // 轮询
                        ins = InsHelper.getSinglePolling();
                    else // 发送队列中的命令帧
                        ins = mWriteQueue.poll();
                    mOutStream.write(ins);
                } catch (IOException | InterruptedException e) {
                    Log.i(TAG, "Reader disconnected with bluetooth");
                    lostConnection();
                }
            }
        }
    }

    /**
     * Bluetooth读数据线程，该线程一直工作在后台，即使没有读写器通过Bluetooth连接
     */
    private class BLEReadTask implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "Begin BLEReadTask");

            byte[] data = new byte[256];
            byte[] inBytes = new byte[256];

            // 循环从Bluetooth的输入流中读取数据，若连接还未建立则休眠指定时间后重试
            int totalCount = 0, length = 0, expandTimes = 0;
            boolean startFlag = false;
            while (true) {
                try {
                    Thread.sleep(1);
                    if (mState != STATE_CONNECTED || mInStream == null) continue;
                    // Read from the InputStream
                    int readCount = mInStream.read(inBytes);
                    for (int i = 0; i < readCount; i++) {
                        if (!startFlag && inBytes[i] == InsHelper.INS_HEADER) {
                            startFlag = true;
                        }
                        if (startFlag) {
                            data[totalCount++] = inBytes[i];
                            if (totalCount == 5) {
                                length += ((data[3] & 0xFF) << 8);
                                length += data[4];
                            }
                            if (totalCount == length + 7) {
                                if ((data[2] & 0xFF) == 0xB6) mIsPowerSet = true; // 设置功率成功
                                if ((data[2] & 0xFF) == 0x0E) mIsQValueSet = true; // 设置Q值成功
                                else if (mInstructionHandler != null)
                                    mInstructionHandler.deal(Arrays.copyOf(data, totalCount)); // 回调函数
                                totalCount = length = 0;
                                startFlag = false;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.i(TAG, "Reader disconnected with bluetooth");
                    lostConnection();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.i(TAG, "Expand read buffer at " + (++expandTimes) + "times");
                    data = new byte[data.length * 10];
                    inBytes = new byte[inBytes.length * 10];
                }  catch (IllegalArgumentException e) {
                    Log.i(TAG, "Bucket EPC error code : " + CommonUtils.bytesToHex(Arrays.copyOf(data, totalCount)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
