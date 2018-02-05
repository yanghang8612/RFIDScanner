package com.casc.rfidscanner;

import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.backend.TagCache;
import com.casc.rfidscanner.backend.TagReader;
import com.casc.rfidscanner.bean.Config;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.message.BatteryStatusMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.utils.ActivityCollector;

import java.util.concurrent.ScheduledExecutorService;

public class MyVars {

    private MyVars(){}

    private static TagReader preReader = null;

    public static TagReader usbReader = null;

    public static TagReader bleReader = null;

    public static ScheduledExecutorService executor = null;

    public static ScheduledExecutorService fragmentExecutor = null;

    public static TagCache cache = null;

    public static Config config = null;

    public static DeliveryBill deliveryBillToShow = null;

    public static RefluxBill refluxBillToShow = null;

    public static BatteryStatusMessage batteryStatus = null;

    public static MultiStatusMessage status = new MultiStatusMessage();

    public static TagReader getReader() {
        if (preReader != null && ActivityCollector.getTopActivity() instanceof ConfigActivity) {
            return preReader;
        }
        if (usbReader.isConnected()) {
            if (preReader != usbReader) {
                preReader = usbReader;
                usbReader.start();
                bleReader.stop();
            }
            return usbReader;
        } else {
            if (preReader != bleReader) {
                preReader = bleReader;
                usbReader.stop();
                bleReader.start();
            }
            return bleReader;
        }
//        switch (LinkType.getType()) {
//            case R0:
//                return usbReader;
//            case R1:
//                return usbReader;
//            case R2:
//                return bleReader;
//            case R3:
//                return bleReader;
//            case R4:
//                return bleReader;
//            case R6:
//                return bleReader;
//            default:
//                return usbReader;
//        }
    }
}
