package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.adapter.GoodsAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RefluxBill {

    private boolean isHighlight;

    private long updatedTime;

    private byte[] cardEPC;

    private String cardID;

    private List<Goods> goods = new ArrayList<>();

    private List<Bucket> buckets = new ArrayList<>();

    public RefluxBill(byte[] cardEPC) {
        this.cardEPC = cardEPC;
        this.cardID = MyVars.config.getCompanySymbol() + "R" + String.format("%03d", ((cardEPC[6] & 0xFF) << 8) + (cardEPC[7] & 0xFF));
    }

    public boolean isHighlight() {
        return isHighlight;
    }

    public void setHighlight(boolean highlight) {
        isHighlight = highlight;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public byte[] getCardEPC() {
        return cardEPC;
    }

    public String getCardID() {
        return cardID;
    }

    public String getCardNum() {
        return cardID.substring(3) + "号";
    }

    public List<Goods> getGoods() {
        return goods;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public int getRefluxCount() {
        return buckets.size();
    }

    public boolean addBucket(Bucket bucket) {
        int index = findMatchedBucketIndex(bucket);
        if (index == -1) {
            buckets.add(0, bucket);
            Goods matchedGoods = findMatchedGoods(bucket);
            if (matchedGoods == null) {
                matchedGoods = new Goods(
                        bucket.getBucketSpec(),
                        bucket.getWaterBrand(),
                        bucket.getWaterSpec(),
                        -1);
                goods.add(matchedGoods);
            }
            matchedGoods.addCurCount();
        }
        return index == -1;
    }

    private int findMatchedBucketIndex(Bucket bucket) {
        for (int i = 0; i < buckets.size(); i++) {
            if (Arrays.equals(buckets.get(i).getEpc(), bucket.getEpc())) {
                return i;
            }
        }
        return -1;
    }

    private Goods findMatchedGoods(Bucket bucket) {
        for (Goods goods : goods) {
            if (goods.isBucketMatched(bucket))
                return goods;
        }
        return null;
    }
}