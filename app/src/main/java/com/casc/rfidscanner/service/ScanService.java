package com.casc.rfidscanner.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.exception.NotSetTagCacheException;
import com.casc.rfidscanner.interfaces.TagCache;
import com.casc.rfidscanner.interfaces.TagScanner;
import com.casc.rfidscanner.utils.CommonUtils;
import com.hoho.android.usbserial.driver.Cp21xxSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Asuka on 2017/11/29.
 */

public class ScanService extends Service implements TagScanner {

    private static final String TAG = ScanService.class.getSimpleName();
    private static final String ACTION_USB_PERMISSION = "INTENT.USB_PERMISSION";

    private boolean isScanning = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();;
    private TagCache tagCache = null;

    private SoundPool soundPool = new SoundPool(10, AudioManager.STREAM_SYSTEM, 5);

    private UsbManager usbManager;
    private UsbDevice usbReader;
    private UsbSerialPort usbReaderPort;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_USB_PERMISSION.equals(intent.getAction())) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    Toast.makeText(ScanService.this, "get permission.", Toast.LENGTH_SHORT).show();
                    try {
                        startScan();
                    } catch (NotSetTagCacheException e) {
                        e.printStackTrace();
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                Toast.makeText(ScanService.this, "attached.", Toast.LENGTH_SHORT).show();
                try {
                    startScan();
                } catch (NotSetTagCacheException e) {
                    e.printStackTrace();
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                Toast.makeText(ScanService.this, "detached.", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate() {
        soundPool.load(this, R.raw.timer, 1);
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(receiver, filter);
        try {
            startScan();
        } catch (NotSetTagCacheException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean connectToReader() {
        return false;
    }

    @Override
    public void disconnectFromReader() {

    }

    @Override
    public boolean isReaderConnected() {
        return false;
    }

    @Override
    public void setTagCache(TagCache cache) {

    }

    @Override
    public void startScan() throws NotSetTagCacheException {
        if (initReader()) {
            showToast("start scan.");
            isScanning = true;
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    byte[] data = new byte[64];
                    byte[] inBytes = new byte[64];
                    //byte[] outBytes = CommonUtils.generateCommandBytes(0x39, "000000000200000008");

                    int totalCount = 0, length = 0;
                    boolean startFlag = false;
                    Map<String, Byte> map = new HashMap<>();
                    try {
                        while (isScanning) {
                            int readCount = usbReaderPort.read(inBytes, 1000);
                            for (int i = 0; i < readCount; i++) {
                                if (!startFlag && inBytes[i] == (byte) 0xBB) {
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
                                            //tagCache.insert(null);
                                            String epc = CommonUtils.bytesToHex(Arrays.copyOfRange(data, 8, 24));
                                            map.put(epc, data[5]);
                                            Log.i(TAG, map.toString());
                                            //soundPool.play(1, 1, 1, 0, 0, 1);
                                        }
                                        totalCount = length = 0;
                                        startFlag = false;
                                        //usbReaderPort.write(outBytes, 1000);
                                        //Thread.sleep(200);
                                    }
                                }
                            }
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            try {
                //usbReaderPort.write(CommonUtils.generateCommandBytes(0xF0, "030601B0"), 1000);
                Thread.sleep(1000);
                //usbReaderPort.write(CommonUtils.generateCommandBytes(0xF3, ""), 1000);
                usbReaderPort.write(CommonUtils.generateCommandBytes(0x27, "22FFFF"), 1000);
            }
            catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stopScan() {
        isScanning = false;
    }

    @Override
    public boolean isScanning() {
        return isScanning;
    }

    private boolean initReader() {
        Map<String, UsbDevice> deviceMap = usbManager.getDeviceList();
        if (!deviceMap.isEmpty()) {
            for (UsbDevice device : deviceMap.values()) {
                usbReader = device;
                Log.i(TAG, device.getDeviceName() + "vid=" + device.getVendorId() + "---pid=" + device.getProductId());
            }
            usbReaderPort = new Cp21xxSerialDriver(usbReader).getPorts().get(0);
            try {
                usbReaderPort.open(usbManager.openDevice(usbReader));
                usbReaderPort.setParameters(115200, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                return true;
            }
            catch (NullPointerException e) {
                // This is very odd.
                usbManager.requestPermission(usbReader, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
            }
            catch (IOException e) {
                Log.e(TAG, "shabi");
            }
        }
        return false;
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
