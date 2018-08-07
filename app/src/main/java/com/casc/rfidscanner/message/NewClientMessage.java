package com.casc.rfidscanner.message;

import com.casc.rfidscanner.bean.Client;

public class NewClientMessage {

    public Client client;

    public NewClientMessage(Client client) {
        this.client = client;
    }
}
