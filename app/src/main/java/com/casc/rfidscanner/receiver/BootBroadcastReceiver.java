package com.casc.rfidscanner.receiver;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.widget.Toast;

import com.casc.rfidscanner.activity.MainActivity;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            Toast.makeText(context, "receive broadcast.", Toast.LENGTH_LONG).show();
            //开机后一般会停留在锁屏页面且短时间内没有进行解锁操作屏幕会进入休眠状态，此时就需要先唤醒屏幕和解锁屏幕
            //屏幕唤醒
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP
                    | PowerManager.SCREEN_DIM_WAKE_LOCK, "StartupReceiver");//最后的参数是LogCat里用的Tag
            wl.acquire();

            //屏幕解锁
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            KeyguardManager.KeyguardLock kl = km.newKeyguardLock("StartupReceiver");//参数是LogCat里用的Tag
            kl.disableKeyguard();

            Intent startIntent = new Intent(context, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);
        }
    }
}
