package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyParams;

public class Server {

    private String lineName;

    private long updateTime;

    public Server() {
        this.updateTime = System.currentTimeMillis();
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public boolean isOnline() {
        return System.currentTimeMillis() - updateTime < MyParams.HEARTBEAT_TIMEOUT;
    }

    public void update() {
        this.updateTime = System.currentTimeMillis();
    }
}
