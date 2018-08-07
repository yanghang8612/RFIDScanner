package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.bean.Client;

public class MessageBillBucket {

    private transient Client client;

    private String cardID;

    private String bucketEPC;

    private boolean isRemoved;

    public MessageBillBucket(String cardID, String bucketEPC, boolean isRemoved) {
        this.cardID = cardID;
        this.bucketEPC = bucketEPC;
        this.isRemoved = isRemoved;
    }

    public Client getClient() {
        return client;
    }

    public MessageBillBucket setClient(Client client) {
        this.client = client;
        return this;
    }

    public String getCardID() {
        return cardID;
    }

    public String getBucketEPC() {
        return bucketEPC;
    }

    public boolean isRemoved() {
        return isRemoved;
    }
}
