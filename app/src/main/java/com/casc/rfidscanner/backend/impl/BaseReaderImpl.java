package com.casc.rfidscanner.backend.impl;

import android.content.Context;
import android.support.annotation.CallSuper;
import android.util.Log;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.InsHelper;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

abstract class BaseReaderImpl implements TagReader {

    private static final String TAG = BaseReaderImpl.class.getSimpleName();

    private static final int SIGNAL_TIMEOUT = 100 * 1000 * 1000;

    // 上下文
    Context mContext;

    // 读写器的连接状态
    int mState = TagReader.STATE_NONE;

    // 读写器的工作状态
    boolean mIsRunning;

    // 是否正在进行同步发送任务
    private boolean mIsExecuteSyncTask;

    // 读写器的MASK
    private byte[] mMask, mResult;

    // 读写器的功率和Q值
    private volatile String mPower, mQValue;

    // 读写器相关设置情况
    private volatile boolean mIsMaskSet = true, mIsSensorSet, mIsSensorChecked, mIsSensorHigh;

    // 返回帧的处理回调函数
    private InsHandler mInsHandler;

    private Lock mLock = new ReentrantLock();

    private Condition mPowerSet = mLock.newCondition();

    private Condition mQValueSet = mLock.newCondition();

    private Condition mMaskSetup = mLock.newCondition();

    private Condition mSensorSetup = mLock.newCondition();

    private Condition mSensorSignal = mLock.newCondition();

    private Condition mReaderSignal  = mLock.newCondition();

    private Condition mInsSuccess  = mLock.newCondition();

    // 读写器命令帧的队列（写队列）
    private ConcurrentLinkedQueue<byte[]> mWriteQueue = new ConcurrentLinkedQueue<>();

    BaseReaderImpl(Context context) {
        mContext = context;
        MyVars.executor.execute(new WriteTask());
        MyVars.executor.execute(new ReadTask());
    }

    @Override
    public void setHandler(InsHandler handler) {
        this.mInsHandler = handler;
    }

    @Override
    public synchronized void sendCommand(byte[] cmd, int times) {
        while (times-- != 0) mWriteQueue.add(cmd);
    }

    @Override
    public synchronized byte[] sendCommandSync(byte[] cmd, int maxTryCount) {
        mIsExecuteSyncTask = true;
        mResult = null;
        while (maxTryCount-- != 0) mWriteQueue.add(cmd);
        mLock.lock();
        try {
            mInsSuccess.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mLock.unlock();
        }
        return mResult;
    }

    @Override
    public void setMask(byte[] mask) {
        mIsMaskSet = false;
        mMask = mask;
    }

    @Override
    public boolean isConnected() {
        return mState == STATE_CONNECTED;
    }

    @Override
    public int getState() {
        return mState;
    }

    @Override
    public void start() {
        mIsRunning = true;
    }

    @Override
    public void pause() {
        mIsRunning = false;
    }

    @Override
    public void stop() {
        mIsRunning = false;
    }

    abstract void write(byte[] data) throws IOException;

    abstract int read(byte[] data) throws IOException;

    @CallSuper
    void lostConnection() {
        if (mState == STATE_CONNECTED) {
            if (mIsExecuteSyncTask) {
                mLock.lock();
                mInsSuccess.signal();
                mLock.unlock();
            }
            mState = STATE_NONE;
            mPower = mQValue = "";
            mIsSensorSet = mIsSensorChecked = mIsSensorHigh = false;
            EventBus.getDefault().post(MyVars.status.setReaderStatus(false));
        }
    }

    /**
     * 写数据线程，该线程一直工作在后台
     */
    private class WriteTask implements Runnable {

        private int mSendCount;

        @Override
        public void run() { // 循环从写队列中取指令帧尝试下发给读写器，若队列为空或读写器尚未连接则休眠指定时间后重试
            Thread.currentThread().setName(
                    BaseReaderImpl.this.getClass().getSimpleName().substring(0, 3)
                            + getClass().getSimpleName());

            while (true) {
                try {
                    if (mState != STATE_CONNECTED) { // 读写器未连接状态下则不发送任何指令
                        Thread.sleep(1);
                    } else if (!ConfigHelper.getParam(MyParams.S_POWER).equals(mPower)) { // 配置发射功率，写任务阻塞
                        write(InsHelper.setTransmitPower(ConfigHelper.getIntegerParam(MyParams.S_POWER)));
                        waitForCondition("Set Power", mPowerSet);
                    } else if (!ConfigHelper.getParam(MyParams.S_Q_VALUE).equals(mQValue)) { // 配置Q值，写任务阻塞
                        write(InsHelper.setQueryParameter(
                                (byte) 0x00, (byte) 0x00, (byte) 0x00,
                                ConfigHelper.getIntegerParam(MyParams.S_Q_VALUE)));
                        waitForCondition("Set Q", mQValueSet);
                    } else if (!mIsMaskSet) { // 设置MASK，写任务阻塞
                        write(InsHelper.getEPCSelectParameter(mMask));
                        waitForCondition("Set Mask", mMaskSetup);
                    } else if (!mIsSensorSet) { // 设置IO端口方向，写任务阻塞
                        write(InsHelper.setSensorIOPort());
                        waitForCondition("Set Sensor", mSensorSetup);
                    } else if (mIsRunning) { // 除却读写器设置外的指令均在读写器工作标志位为true时才下发
                        if (ConfigHelper.getBooleanParam(MyParams.S_SENSOR_SWITCH)
                                && (!mIsSensorChecked || !mIsSensorHigh)) { // 检测传感器电平，写任务阻塞
                            write(InsHelper.getSensorIOPort());
                            waitForCondition("Get IO", mSensorSignal);
                        } else if (ConfigHelper.getIntegerParam(MyParams.S_TIME) == mSendCount) { // 完成一个Round的发送，重置状态并Rest指定时间
                            mSendCount = 0;
                            mIsSensorChecked = false;
                            Thread.sleep(ConfigHelper.getIntegerParam(MyParams.S_REST));
                        } else if (!mWriteQueue.isEmpty()) { // 队列不空则取队首指令发送，写任务阻塞
                            write(mWriteQueue.poll());
                            waitForCondition("Queue Ins", mReaderSignal);
                        } else { // 不满足以上条件则发送单次轮询指令，写任务不阻塞
                            mSendCount += 1;
                            write(InsHelper.getSinglePolling());
                            Thread.sleep(ConfigHelper.getIntegerParam(MyParams.S_INTERVAL));
                        }
                    } else {
                        Thread.sleep(1);
                    }
                } catch (IOException | NullPointerException e) {
                    Log.i(TAG, "Reader disconnected");
                    e.printStackTrace();
                    lostConnection();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void waitForCondition(String comment, Condition condition) throws InterruptedException {
            mLock.lock();
            condition.awaitNanos(SIGNAL_TIMEOUT);
//            Log.i(TAG, comment + " cost: " + ((SIGNAL_TIMEOUT - condition.awaitNanos(SIGNAL_TIMEOUT)) / 1000 / 1000));
            mLock.unlock();
        }
    }

    /**
     * 读数据线程，该线程一直工作在后台
     */
    private class ReadTask implements Runnable {

        @Override
        public void run() {
            Thread.currentThread().setName(
                    BaseReaderImpl.this.getClass().getSimpleName().substring(0, 3)
                            + getClass().getSimpleName());

            byte[] data = new byte[512];
            byte[] temp = new byte[256];

            int leftCount = 0, totalCount = 0;
            while (true) {
                try {
                    Thread.sleep(1);
                    if (mState != STATE_CONNECTED) continue;
                    //long start = System.nanoTime();
                    totalCount = leftCount + read(temp);
                    //Log.i(TAG, "Read Cost:" + (System.nanoTime() - start));
                    System.arraycopy(temp, 0, data, leftCount, totalCount - leftCount);
                    leftCount = totalCount;
                    for (int i = 0; i < totalCount; i++) {
                        if (data[i] == InsHelper.INS_HEADER) {
                            int j = i + 1;
                            while (j < totalCount && data[j] != InsHelper.INS_END) j++;
                            if (j < totalCount && InsHelper.checkIns(data, i, j)) { // 找到了一条指令，包含指令的开头和结尾，检查指令的合法性
                                switch (data[i + 2] & 0xFF) {
                                    case 0xB6:
                                        mPower = ConfigHelper.getParam(MyParams.S_POWER);
                                        sendSignal(mPowerSet);
                                        break;
                                    case 0x0E:
                                        mQValue = ConfigHelper.getParam(MyParams.S_Q_VALUE);
                                        sendSignal(mQValueSet);
                                        break;
                                    case 0x0C:
                                        mIsMaskSet = true;
                                        sendSignal(mMaskSetup);
                                        break;
                                    case 0x1A:
                                        if (data[i + 5] == 0x00) {
                                            mIsSensorSet = data[i + 7] == 0x01;
                                            sendSignal(mSensorSetup);
                                        } else {
                                            mIsSensorChecked = true;
                                            mIsSensorHigh = data[i + 7] == 0x01;
                                            if (mInsHandler != null)
                                                mInsHandler.sensorSignal(mIsSensorHigh);
                                            sendSignal(mSensorSignal);
                                        }
                                        break;
                                    case 0x22: // 轮询通知帧
                                        if (mInsHandler != null)
                                            mInsHandler.dealIns(Arrays.copyOfRange(data, i, j + 1));
                                        break;
                                    case 0xFF:
                                        if (data[5] == 0x15 || !mIsExecuteSyncTask) {
                                            if (mInsHandler != null)
                                                mInsHandler.dealIns(Arrays.copyOfRange(data, i, j + 1));
                                        } else {
                                            if (MyParams.PRINT_COMMAND) {
                                                Log.i(TAG, CommonUtils.bytesToHex(Arrays.copyOfRange(data, i, j + 1)));
                                            }
                                            sendSignal(mReaderSignal);
                                            if (mWriteQueue.isEmpty()) {
                                                sendSignal(mInsSuccess);
                                                mIsExecuteSyncTask = false;
                                            }
                                        }
                                        break;
                                    default:
                                        if (MyParams.PRINT_COMMAND) {
                                            Log.i(TAG, CommonUtils.bytesToHex(Arrays.copyOfRange(data, i, j + 1)));
                                        }
                                        if (!mIsExecuteSyncTask) {
                                            if (mInsHandler != null)
                                                mInsHandler.dealIns(Arrays.copyOfRange(data, i, j + 1));
                                        } else {
                                            mWriteQueue.clear();
                                            mResult = Arrays.copyOfRange(data, i, j + 1);
                                            sendSignal(mReaderSignal);
                                            sendSignal(mInsSuccess);
                                            mIsExecuteSyncTask = false;
                                        }
                                }
                                leftCount -= j - i + 1;
                                i = j;
                            }
                        }
                    }
                    System.arraycopy(data, totalCount - leftCount, data, 0, leftCount);
                } catch (IOException | NullPointerException e) {
                    Log.i(TAG, "Reader disconnected");
                    e.printStackTrace();
                    lostConnection();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Log.i(TAG, "Expand read buffer");
                    data = new byte[data.length * 10];
                    temp = new byte[temp.length * 10];
                } catch (IllegalArgumentException e) {
                    Log.i(TAG, "Bucket EPC error code : " + CommonUtils.bytesToHex(Arrays.copyOf(data, totalCount)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void sendSignal(Condition signal) {
            mLock.lock();
            signal.signal();
            mLock.unlock();
        }
    }
}
