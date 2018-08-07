package com.casc.rfidscanner.bean;

import android.text.TextUtils;

import com.casc.rfidscanner.MyParams;

import java.util.ArrayList;
import java.util.List;

public class Client {

    private String name;

    private String ipStr;

    private long updateTime;

    private boolean isDataIncoming;

    private List<DeliveryBill> bills = new ArrayList<>();

    public Client(String name, String ipStr) {
        this.name = name;
        this.ipStr = ipStr;
        this.updateTime = System.currentTimeMillis();
    }

    public String getName() {
        return TextUtils.isEmpty(name) ? ipStr : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIPStr() {
        return ipStr;
    }

    public boolean isOnline() {
        return System.currentTimeMillis() - updateTime < MyParams.HEARTBEAT_TIMEOUT;
    }

    public void update() {
        this.updateTime = System.currentTimeMillis();
    }

    public synchronized boolean isDataIncoming() {
        return isDataIncoming;
    }

    public synchronized void setDataIncoming(boolean dataIncoming) {
        isDataIncoming = dataIncoming;
    }

    public List<DeliveryBill> getBills() {
        return bills;
    }

    public void addBill(DeliveryBill bill) {
        bills.add(bill);
    }
}
