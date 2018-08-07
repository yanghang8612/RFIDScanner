package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Client;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.helper.ConfigHelper;

import java.util.ArrayList;
import java.util.List;

public class MessageBillDelivery {

    private transient Client client;

    private String card;

    private String bill;

    private List<MessageBucket> buckets = new ArrayList<>();

    public MessageBillDelivery(DeliveryBill bill) {
        this.card = bill.getCardStr();
        this.bill = bill.getBillStr();
        for (Bucket bucket : bill.getBuckets()) {
            buckets.add(new MessageBucket(bucket.getEpcStr(), bucket.getTime()));
        }
    }

    public Client getClient() {
        return client;
    }

    public MessageBillDelivery setClient(Client client) {
        this.client = client;
        return this;
    }

    public String getCard() {
        return card;
    }

    public String getBill() {
        return bill;
    }

    public List<MessageBucket> getBuckets() {
        return buckets;
    }
}
