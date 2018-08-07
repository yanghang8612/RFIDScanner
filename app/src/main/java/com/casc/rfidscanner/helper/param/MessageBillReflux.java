package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.bean.Client;
import com.casc.rfidscanner.bean.RefluxBill;

import java.util.ArrayList;
import java.util.List;

public class MessageBillReflux {

    private transient Client client;

    private String card;

    private List<MessageBucket> buckets = new ArrayList<>();

    public MessageBillReflux(RefluxBill bill) {
        this.card = bill.getCardStr();
    }

    public Client getClient() {
        return client;
    }

    public MessageBillReflux setClient(Client client) {
        this.client = client;
        return this;
    }

    public String getCard() {
        return card;
    }

    public List<MessageBucket> getBuckets() {
        return buckets;
    }
}
