package com.casc.rfidscanner.bean;

public class Goods {

    // 产品名称
    private ProductInfo info;

    // 总数量
    private int totalCount;

    // 成垛32数量
    private int stack32Count;

    // 成垛32数量
    private int stack48Count;

    // 成垛数量
    private int singleCount;

    // 成垛数量
    private int backCount;

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

    public int getLeftCount() {
        return Math.max(0, totalCount - getCurCount());
    }

    public int getCurCount() {
        return 32 * stack32Count + 48 * stack48Count + singleCount - backCount;
    }

    public int getStackCount() {
        return stack32Count + stack48Count;
    }

    public int getSingleCount() {
        return singleCount;
    }

    public int getBackCount() {
        return backCount;
    }

    public void addStack32() {
        this.stack32Count++;
    }

    public void minusStack32() {
        this.stack32Count--;
    }

    public void addStack48() {
        this.stack48Count++;
    }

    public void minusStack48() {
        this.stack48Count--;
    }

    public void addSingle() {
        this.singleCount++;
    }

    public void minusSingle() {
        this.singleCount--;
    }

    public void addBack() {
        this.backCount++;
    }

    public void minusBack() {
        this.backCount--;
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
