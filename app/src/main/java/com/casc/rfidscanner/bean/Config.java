package com.casc.rfidscanner.bean;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private String companycode;

    private String companysymbol;

    private List<Specify> bucketspecinfo = new ArrayList<>();

    private List<Specify> buckettypeinfo = new ArrayList<>();

    private List<Specify> waterbrandinfo = new ArrayList<>();

    private List<Specify> waterspecinfo = new ArrayList<>();

    private List<Specify> bucketproducerinfo = new ArrayList<>();

    private List<Specify> bucketownerinfo = new ArrayList<>();

    private List<Specify> bucketuserinfo = new ArrayList<>();

    private List<String> dealerinfo = new ArrayList<>();

    private List<String> driverinfo = new ArrayList<>();

    public String getCompanyCode() {
        return companycode;
    }

    public String getCompanySymbol() {
        return companysymbol;
    }

    public List<Specify> getBucketSpecInfo() {
        return bucketspecinfo;
    }

    public String getBucketSpecByCode(byte code) {
        return findSpecifyByCode(bucketspecinfo, code, "BucketSpec");
    }

    public byte getCodeByBucketSpec(String bucketSpec) {
        return findCodeBySpecify(bucketspecinfo, bucketSpec, "BucketSpec");
    }

    public List<Specify> getBucketTypeInfo() {
        return buckettypeinfo;
    }

    public String getBucketTypeByCode(byte code) {
        return findSpecifyByCode(buckettypeinfo, code, "BucketType");
    }

    public byte getCodeByBucketType(String bucketType) {
        return findCodeBySpecify(buckettypeinfo, bucketType, "BucketType");
    }

    public List<Specify> getWaterBrandInfo() {
        return waterbrandinfo;
    }

    public String getWaterBrandByCode(byte code) {
        return findSpecifyByCode(waterbrandinfo, code, "WaterBrand");
    }

    public byte getCodeByWaterBrand(String waterBrand) {
        return findCodeBySpecify(waterbrandinfo, waterBrand, "WaterBrand");
    }

    public List<Specify> getWaterSpecInfo() {
        return waterspecinfo;
    }

    public String getWaterSpecByCode(byte code) {
        return findSpecifyByCode(waterspecinfo, code, "WaterSpec");
    }

    public byte getCodeByWaterSpec(String waterSpec) {
        return findCodeBySpecify(waterspecinfo, waterSpec, "WaterSpec");
    }

    public List<Specify> getBucketProducerInfo() {
        return bucketproducerinfo;
    }

    public String getBucketProducerByCode(byte code) {
        return findSpecifyByCode(bucketproducerinfo, code, "BucketProducer");
    }

    public byte getCodeByBucketProducer(String bucketProducer) {
        return findCodeBySpecify(bucketproducerinfo, bucketProducer, "BucketProducer");
    }

    public List<Specify> getBucketOwnerInfo() {
        return bucketownerinfo;
    }

    public String getBucketOwnerByCode(byte code) {
        return findSpecifyByCode(bucketownerinfo, code, "BucketOwner");
    }

    public byte getCodeByBucketOwner(String bucketOwner) {
        return findCodeBySpecify(bucketownerinfo, bucketOwner, "BucketOwner");
    }

    public List<Specify> getBucketUserInfo() {
        return bucketuserinfo;
    }

    public String getBucketUserByCode(byte code) {
        return findSpecifyByCode(bucketuserinfo, code, "BucketUser");
    }

    public byte getCodeByBucketUser(String bucketUser) {
        return findCodeBySpecify(bucketuserinfo, bucketUser, "BucketUser");
    }

    private String findSpecifyByCode(List<Specify> specifies, byte code, String name) {
        for (Specify specify : specifies) {
            if (specify.getCode() == code) {
                return specify.getSpecify();
            }
        }
        throw new IllegalArgumentException("Find " + name + " by code " + code + " error: no such match code.");
    }

    private byte findCodeBySpecify(List<Specify> specifies, String target, String name) {
        for (Specify specify : specifies) {
            if (specify.getSpecify().equals(target)) {
                return specify.getCode();
            }
        }
        throw new IllegalArgumentException("Find code by " + name + " error: no matched " + name + ".");
    }

    public List<String> getDealerInfo() {
        return dealerinfo;
    }

    public List<String> getDriverInfo() {
        return driverinfo;
    }
}
