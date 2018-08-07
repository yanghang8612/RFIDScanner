package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.bean.Client;

/**
 * 为啥不做成内部类，因为要便于HttpServer间的通讯做遍历处理
 */
public class MessageBucket {

    private transient Client client;

    private String epc;

    private long time;

    public MessageBucket(String epc) {
        this(epc, System.currentTimeMillis());
    }

    public MessageBucket(String epc, long time) {
        this.time = time;
        this.epc = epc;
    }

    public Client getClient() {
        return client;
    }

    public MessageBucket setClient(Client client) {
        this.client = client;
        return this;
    }

    public String getEPC() {
        return epc;
    }

    public long getTime() {
        return time;
    }
}
