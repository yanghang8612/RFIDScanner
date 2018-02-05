package com.casc.rfidscanner.bean;

public class Goods {

    // 桶规格
    private String bucketSpec;

    // 水品牌
    private String waterBrand;

    // 水规格
    private String waterSpec;

    // 总数量
    private int totalCount;

    // 出库数量
    private int curCount;

    public Goods(String bucketSpec, String waterBrand, String waterSpec, int totalCount) {
        this.bucketSpec = bucketSpec;
        this.waterBrand = waterBrand;
        this.waterSpec = waterSpec;
        this.totalCount = totalCount;
    }

    public String getBucketSpec() {
        return bucketSpec;
    }

    public String getWaterBrand() {
        return waterBrand;
    }

    public String getWaterSpec() {
        return waterSpec;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getCurCount() {
        return curCount;
    }

    public void addCurCount() {
        this.curCount++;
    }

    public void minusCurCount() {
        this.curCount--;
    }

    public boolean isBucketMatched(Product bucket) {
        return bucketSpec.equals(bucket.getBucketSpec()) &&
                waterBrand.equals(bucket.getWaterBrand()) &&
                waterSpec.equals(bucket.getWaterSpec());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        boolean isEqual = false;
        if (obj instanceof Goods) {
            Goods other = (Goods) obj;
            isEqual = this.bucketSpec.equals(other.bucketSpec) &&
                    this.waterBrand.equals(other.waterBrand) &&
                    this.waterSpec.equals(other.waterSpec);
            if (isEqual) this.totalCount += other.totalCount;
        }
        return isEqual;
    }
}
