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

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.helper.net.param.MsgLog;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * TagReader的Bluetooth实现
 */
public class BLEReaderImpl extends BaseReaderImpl {

    private static final String TAG = BLEReaderImpl.class.getSimpleName();

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // 串口

    // 循环发现Timer
    private DiscoveryTimer mDiscoveryTimer;

    // 读写器蓝牙相关字段
    private BluetoothAdapter mBLEAdapter;
    private BluetoothDevice mBLEDevice;
    private BluetoothSocket mBLESocket;
    private InputStream mInStream; // 蓝牙输入流
    private OutputStream mOutStream; // 蓝牙输出流

    private String mPreErrorMessage;

    public BLEReaderImpl(Context context) {
        super(context);
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
                    if (mIsRunning && mState == STATE_NONE && "UL05".equals(scanDevice.getName())) {
                        mBLEDevice = scanDevice;
                        MyVars.executor.execute(new BLEConnectTask());
                    }
                }
            }
        };
        context.registerReceiver(mReceiver, filter);
    }

    @Override
    void write(byte[] data) throws IOException {
        mOutStream.write(data);
    }

    @Override
    int read(byte[] data) throws IOException {
        return mInStream.read(data);
    }

    @Override
    synchronized void lostConnection() {
        super.lostConnection();
        try {
            mBLESocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception ignored) {
        } finally {
            mBLESocket = null;
            startDiscovery();
        }
    }

    @Override
    public void start() {
        super.start();
        if (mBLESocket != null && !mBLESocket.isConnected() || mBLEDevice == null) {
            if (mState == STATE_CONNECTED) {
                lostConnection();
            } else {
                startDiscovery();
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mState == STATE_CONNECTED)
            lostConnection();
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
            try {
                mBLESocket = mBLEDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                mBLESocket.connect();
                mInStream = mBLESocket.getInputStream();
                mOutStream = mBLESocket.getOutputStream();
                mState = STATE_CONNECTED;
                MyVars.cache.storeLogMessage(MsgLog.info(
                        "读写器已连接（by蓝牙），用时" + (System.currentTimeMillis() - startTime) + "ms"));
                SpeechSynthesizer.getInstance().speak("读写器已连接");
                EventBus.getDefault().post(MyVars.status.setReaderStatus(true));
                Log.i(TAG, "BLEConnectTask cost " + (System.currentTimeMillis() - startTime));
            } catch (IOException e) {
                if (!e.getMessage().equals(mPreErrorMessage)) {
                    mPreErrorMessage = e.getMessage();
                    MyVars.cache.storeLogMessage(MsgLog.error("蓝牙连接异常：" + e.getMessage()));
                }
                e.printStackTrace();
                mState = STATE_NONE;
                if (!mDiscoveryTimer.isRunning) {
                    startDiscovery();
                }
            }
        }
    }

    private void startDiscovery() {
        if (mIsRunning && mState == STATE_NONE) {
            Log.i(TAG, "BLE start discovery");
            if (mBLEAdapter.isDiscovering()) mBLEAdapter.cancelDiscovery();
            mBLEAdapter.startDiscovery();
            mDiscoveryTimer.cancel();
            mDiscoveryTimer.startTimer();
        }
    }

    private class DiscoveryTimer extends CountDownTimer {

        private boolean isRunning;

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
            Log.i(TAG, "BLE finish discovery");
            mBLEAdapter.cancelDiscovery();
            isRunning = false;
            startDiscovery();
        }


        public synchronized void cancelTimer() {
            isRunning = false;
            cancel();
        }

        public synchronized void startTimer() {
            isRunning = true;
            start();
        }

        public boolean isRunning() {
            return isRunning;
        }
    }
}
