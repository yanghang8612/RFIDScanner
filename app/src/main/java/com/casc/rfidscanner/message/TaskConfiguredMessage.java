package com.casc.rfidscanner.message;

public class TaskConfiguredMessage {

    public String productName;

    public int productCount;

    public int amount;

    public TaskConfiguredMessage(String productName, int productCount, int amount) {
        this.productName = productName;
        this.productCount = productCount;
        this.amount = amount;
    }
}
