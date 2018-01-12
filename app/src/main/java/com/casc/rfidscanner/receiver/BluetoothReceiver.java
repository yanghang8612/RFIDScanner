package com.casc.rfidscanner.receiver;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.casc.rfidscanner.utils.ClsUtils;

import java.util.Date;

/**
 * 蓝牙自动配对
 */
public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = BluetoothReceiver.class.getSimpleName();

    private static final String PIN = "0000";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
            BluetoothDevice mBluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            try {
                // 确认配对
                // ClsUtils.setPairingConfirmation(mBluetoothDevice.getClass(), mBluetoothDevice, true);
                // 终止有序广播
                abortBroadcast();// 如果没有将广播终止，则会出现一个一闪而过的配对框。
                // 调用setPin方法进行配对
                boolean ret = ClsUtils.setPin(mBluetoothDevice.getClass(), mBluetoothDevice, PIN);

                Log.e(TAG, "PAIRING_REQUEST done!( MAC: " + mBluetoothDevice.getAddress() + ")");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
