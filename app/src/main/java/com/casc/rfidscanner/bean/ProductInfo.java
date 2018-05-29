package com.casc.rfidscanner.bean;

public class ProductInfo {

    private int productcode;

    private String productname;

    public int getCode() {
        return productcode;
    }

    public String getName() {
        return productname;
    }

    @Override
    public String toString() {
        return productname;
    }
}
