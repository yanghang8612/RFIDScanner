package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;

public class RegisteredCount {

    private ProductInfo productInfo;

    private int count;

    public RegisteredCount(String productName) {
        this.productInfo = MyVars.config.getProductInfoByName(productName);
        this.count = 1;
    }

    public String getProductName() {
        return productInfo.getName();
    }

    public int getProductCode() {
        return productInfo.getCode();
    }

    public int getCount() {
        return count;
    }

    public void addCount() {
        count += 1;
    }
}
