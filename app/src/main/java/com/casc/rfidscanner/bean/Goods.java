package com.casc.rfidscanner.bean;

public class Goods {

    // 产品名称
    private ProductInfo info;

    // 总数量
    private int totalCount;

    // 出库数量
    private int curCount;

    public Goods(ProductInfo info, int totalCount) {
        this.info = info;
        this.totalCount = totalCount;
    }

    public int getCode() {
        return info.getCode();
    }

    public String getName() {
        return info.getName();
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void addTotalCount(int count) {
        totalCount += count;
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

    public boolean isBucketMatched(Bucket bucket) {
        return bucket.getProductInfo().getCode() == info.getCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof Goods) {
            Goods other = (Goods) obj;
            return this.info.getCode() == other.info.getCode();
        }
        return false;
    }
}
