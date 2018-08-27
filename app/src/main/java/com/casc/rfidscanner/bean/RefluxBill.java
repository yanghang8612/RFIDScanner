package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.adapter.GoodsAdapter;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RefluxBill {

    private boolean isHighlight;

    private long updatedTime;

    private byte[] card;

    private String cardID;

    private List<Goods> goods = new ArrayList<>();

    private Map<String, Bucket> buckets = new LinkedHashMap<>();

    private GoodsAdapter goodsAdapter;

    public RefluxBill(byte[] card) {
        this.card = card;
        this.cardID =
                MyVars.config.getCompanySymbol() + "R"
                        + String.format("%03d", ((card[5] & 0xFF) << 8) + (card[6] & 0xFF));
        this.goodsAdapter = new GoodsAdapter(goods, false);
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

    public byte[] getCard() {
        return card;
    }

    public String getCardStr() {
        return CommonUtils.bytesToHex(card);
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
        return new ArrayList<>(buckets.values());
    }

    public GoodsAdapter getGoodsAdapter() {
        return goodsAdapter;
    }

    public int getRefluxCount() {
        return buckets.size();
    }

    public boolean addBucket(String bucketEPC) {
        if (!buckets.containsKey(bucketEPC)) {
            Bucket bucket = new Bucket(bucketEPC);
            buckets.put(bucketEPC, bucket);
            Goods matchedGoods = findMatchedGoods(bucket);
            if (matchedGoods == null) {
                matchedGoods = new Goods(bucket.getProductInfo(), -1);
                goods.add(matchedGoods);
            }
            matchedGoods.addCurCount();
            return true;
        }
        return false;
    }

    public boolean addBucket(Bucket bucket) {
        return addBucket(bucket.getEpcStr());
    }

    private Goods findMatchedGoods(Bucket bucket) {
        for (Goods goods : goods) {
            if (goods.isBucketMatched(bucket))
                return goods;
        }
        return null;
    }
}