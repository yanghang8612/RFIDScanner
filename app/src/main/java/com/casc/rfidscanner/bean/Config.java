package com.casc.rfidscanner.bean;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private int versionnumber;

    private String industrysymbol;

    private String companysymbol;

    private List<String> dealerinfo = new ArrayList<>();

    private List<String> driverinfo = new ArrayList<>();

    private List<ProductInfo> productinfo = new ArrayList<>();

    private List<DisableInfo> disableinfo = new ArrayList<>();

    public int getVersionNumber() {
        return versionnumber;
    }

    public String getHeader() {
        return industrysymbol + companysymbol;
    }

    public String getIndustrySymbol() {
        return industrysymbol;
    }

    public String getCompanySymbol() {
        return companysymbol;
    }

    public List<String> getDealerInfo() {
        return dealerinfo == null ? dealerinfo = new ArrayList<>() : dealerinfo;
    }

    public List<String> getDriverInfo() {
        return driverinfo == null ? driverinfo = new ArrayList<>() : driverinfo;
    }

    public List<ProductInfo> getProductInfo() {
        return productinfo == null ? productinfo = new ArrayList<>() : productinfo;
    }

    public ProductInfo getProductInfoByCode(int code) {
        for (ProductInfo info : productinfo) {
            if (info.getCode() == code) {
                return info;
            }
        }
        return null;
    }

    public ProductInfo getProductInfoByName(String name) {
        for (ProductInfo info : productinfo) {
            if (info.getName().equals(name)) {
                return info;
            }
        }
        return null;
    }

    public List<DisableInfo> getDisableInfo() {
        return disableinfo == null ? disableinfo = new ArrayList<>() : disableinfo;
    }

    public DisableInfo getDisableInfoByCode(int code) {
        for (DisableInfo info : disableinfo) {
            if (info.getCode() == code) {
                return info;
            }
        }
        return null;
    }

    public DisableInfo getDisableInfoByWord(String word) {
        for (DisableInfo info : disableinfo) {
            if (info.getWord().equals(word)) {
                return info;
            }
        }
        return null;
    }
}
