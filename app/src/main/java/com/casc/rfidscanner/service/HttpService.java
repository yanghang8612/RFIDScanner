package com.casc.rfidscanner.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.casc.rfidscanner.backend.HttpServer;

import java.io.IOException;

public class HttpService extends Service {

    private static String TAG = HttpService.class.getSimpleName();

    private HttpServer server;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        server = new HttpServer(8888);
        try {
            server.start();
            Log.i(TAG, "Start HttpServer at 8888");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            server.stop();
            Log.i(TAG, "Stop HttpServer at 8888");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
