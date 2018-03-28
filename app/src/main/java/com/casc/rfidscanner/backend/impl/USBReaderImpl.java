package com.casc.rfidscanner.backend.impl;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.utils.CommonUtils;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * TagReader的USB实现
 */
public class USBReaderImpl implements TagReader {

    private static final String TAG = USBReaderImpl.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "INTENT.USB_PERMISSION";

    private static final int BAUD_RATE = 115200;

    private static final int DEFAULT_VID = 0x10c4;
    private static final int DEFAULT_PID = 0xea60;

    // 用于注册BroadcastReceiver的上下文
    private Context mContext;

    // 返回帧的处理回调函数
    private InstructionHandler mInstructionHandler;

    // 读写器的连接状态
    private int mState = STATE_NONE;

    // 读写器的初始化状态
    private volatile boolean mIsPowerSet, mIsQValueSet;

    // 读写器的工作状态
    private volatile boolean mIsRunning;

    // 读写器命令帧的队列（写队列）
    private ConcurrentLinkedQueue<byte[]> mWriteQueue = new ConcurrentLinkedQueue<>();

    // 读写器USB相关的字段
    private UsbManager mUsbManager;
    private UsbDevice mUsbDevice;
    private UsbSerialPort mSerialPort;

    public USBReaderImpl(ContextWrapper context) {
        this.mContext = context;
        this.mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        MyVars.executor.execute(new USBWriteTask());
        MyVars.executor.execute(new USBReadTask());

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                    boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    if (granted) {
                        buildConnection();
                    }
                } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                    buildConnection();
                } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                    lostConnection();
                }
            }
        };
        mContext.registerReceiver(receiver, filter);

        // 尝试建立连接
        buildConnection();
    }

    /**
     * 通过USB建立到读写器的连接
     */
    private void buildConnection() {
        // 获取到设备列表
        Map<String, UsbDevice> deviceMap = mUsbManager.getDeviceList();
        if (deviceMap != null && !deviceMap.isEmpty()) {
            for (UsbDevice device : deviceMap.values()) {
                if (device.getVendorId() == DEFAULT_VID && device.getProductId() == DEFAULT_PID) {
                    Log.i(TAG, device.getDeviceName() + " with vid = " + device.getVendorId() + " AND pid = " + device.getProductId());
                    mUsbDevice = device;
                }
            }
        }
        // 尝试连接到设备，并打开Serial端口，打开失败则提示用户获取权限
        if (mUsbDevice != null) {
            try {
                mSerialPort = new Cp21xxSerialDriver(mUsbDevice).getPorts().get(0);
                mSerialPort.open(mUsbManager.openDevice(mUsbDevice));
                mSerialPort.setParameters(BAUD_RATE, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                if(mState != STATE_CONNECTED)
                    SpeechSynthesizer.getInstance().speak("读写器已连接");
                mState = STATE_CONNECTED;
//                if (mIsRunning)
//                    EventBus.getDefault().post(MyVars.status.setReaderStatus(true));
            }catch (NullPointerException e) {
                Log.i(TAG, "Need permission when build connection with reader by usb");
                mUsbManager.requestPermission(mUsbDevice, PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0));
            }
            catch (IOException e) {
                Log.i(TAG, "IOException when connect reader with usb");
            }
        }
    }

    /**
     * 失去连接的处理
     */
    private void lostConnection() {
        if (mState == STATE_CONNECTED) {
            mState = STATE_NONE;
            SpeechSynthesizer.getInstance().speak("读写器断开连接");
            try {
                mSerialPort.close();
            } catch (Exception e) {
                Log.i(TAG, "Close usb serial port error");
            }
            mUsbDevice = null;
            MyVars.status.setReaderStatus(false);
//            if (mIsRunning)
//                EventBus.getDefault().post(MyVars.status);

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
        if (!mIsRunning || mState == STATE_NONE || mSerialPort == null) return;
        while (times-- != 0) mWriteQueue.add(cmd);
    }

    @Override
    public boolean isConnected() {
        return mState == STATE_CONNECTED;
    }

    @Override
    public void start() {
        mIsRunning = true;
        buildConnection();
    }

    @Override
    public void resume() {
        mIsRunning = true;
    }

    @Override
    public void pause() {
        mIsRunning = false;
    }

    @Override
    public void stop() {
        mIsRunning = false;
        lostConnection();
    }

    /**
     * USB写数据线程，该线程一直工作在后台，即使没有读写器通过USB连接
     */
    private class USBWriteTask implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "Begin USBWriteTask");

            // 循环从写队列中取指令帧尝试下发给读写器，若队列为空或读写器尚未连接则休眠指定时间后重试
            while (true) {
                try {
                    Thread.sleep(LinkType.getSendInterval());
                    if (!mIsRunning || mState == STATE_NONE || mSerialPort == null) continue;
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
                    mSerialPort.write(ins, 0);
                } catch (IOException | NullPointerException e) {
                    Log.i(TAG, "Reader disconnected with usb");
                    lostConnection();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * USB读数据线程，该线程一直工作在后台，即使没有读写器通过USB连接
     */
    private class USBReadTask implements Runnable {

        @Override
        public void run() {
            Log.i(TAG, "Begin USBReadTask");

            byte[] data = new byte[256];
            byte[] inBytes = new byte[256];

            // 循环从USB的UART端口中读取数据，若连接还未建立则休眠指定时间后重试
            int totalCount = 0, length = 0, expandTimes = 0;
            boolean startFlag = false;
            while (true) {
                try {
                    Thread.sleep(1);
                    if (mState == STATE_NONE || mSerialPort == null) continue;
                    int readCount = mSerialPort.read(inBytes, 0);
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
                                else if(mInstructionHandler != null) {
                                    byte[] ins = Arrays.copyOf(data, totalCount);
                                    if(MyParams.PRINT_COMMAND)
                                        Log.i(TAG, CommonUtils.bytesToHex(ins));
                                    mInstructionHandler.deal(ins); // 回调函数
                                }
                                totalCount = length = 0;
                                startFlag = false;
                            }
                        }
                    }
                } catch (IOException | NullPointerException e) {
                    Log.i(TAG, "Reader disconnected from usb");
                    e.printStackTrace();
                    lostConnection();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.i(TAG, "Expand read buffer at " + (++expandTimes) + "times");
                    data = new byte[data.length * 10];
                    inBytes = new byte[inBytes.length * 10];
                } catch (IllegalArgumentException e) {
                    Log.i(TAG, "Bucket EPC error code : " + CommonUtils.bytesToHex(Arrays.copyOf(data, totalCount)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
