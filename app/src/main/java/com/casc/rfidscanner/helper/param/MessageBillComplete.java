package com.casc.rfidscanner.helper.param;

import com.casc.rfidscanner.bean.Client;

public class MessageBillComplete {

    private transient Client client;

    private String cardID;

    public MessageBillComplete(String cardID) {
        this.cardID = cardID;
    }

    public Client getClient() {
        return client;
    }

    public MessageBillComplete setClient(Client client) {
        this.client = client;
        return this;
    }

    public String getCardID() {
        return cardID;
    }
}
