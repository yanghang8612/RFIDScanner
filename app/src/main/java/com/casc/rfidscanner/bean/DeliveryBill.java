package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.adapter.GoodsAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DeliveryBill {

    public static String getCardIDFromEPC(byte[] card) {
        int cardNo = 0;
        cardNo += (card[5] & 0xFF) << 8;
        cardNo += (card[6] & 0xFF);
        return MyVars.config.getCompanySymbol() + "C" + String.format("%03d", cardNo);
    }

    private boolean isHighlight;

    private String billID;

    private String dealer;

    private String driver;

    private long updatedTime = System.currentTimeMillis();

    private List<Goods> goods = new ArrayList<>();

    private Map<String, Bucket> buckets = new LinkedHashMap<>();

    private Set<String> removes = new HashSet<>();

    private GoodsAdapter goodsAdapter = new GoodsAdapter(goods, true);

    public DeliveryBill(String billID, String dealer, String driver) {
        this.billID = billID;
        this.dealer = dealer;
        this.driver = driver;
    }

    public DeliveryBill(byte[] card) {
        // 解析出库专用卡EPC
        int cardNo = 0;
        cardNo += (card[5] & 0xFF) << 8;
        cardNo += (card[6] & 0xFF);
        this.billID = MyVars.config.getCompanySymbol() + "C" + String.format("%03d", cardNo);
        this.dealer = "无";
        this.driver = "无";
    }

    public String getBillID() {
        return billID;
    }

    public boolean isFromCard() {
        return billID.length() == 6;
    }

    public String getDealer() {
        return dealer;
    }

    public String getDriver() {
        return driver;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    public List<Bucket> getBuckets() {
        return new ArrayList<>(buckets.values());
    }

    public List<String> getRemoves() {
        return new ArrayList<>(removes);
    }

    public GoodsAdapter getGoodsAdapter() {
        return goodsAdapter;
    }

    public int getDeliveryCount() {
        return buckets.size();
    }

    public boolean addBucket(String epcStr) {
        if (!buckets.containsKey(epcStr)) {
            Bucket bucket = new Bucket(epcStr);
            buckets.put(epcStr, bucket);
            addMatchedGoods(bucket);
            return true;
        }
        return false;
    }

    public boolean removeBucket(String epcStr) {
        removes.add(epcStr);
        if (buckets.containsKey(epcStr)) {
            removeMatchedGoods(buckets.remove(epcStr));
            return true;
        }
        return false;
    }

    public void addGoods(int code, int quantity) {
        for (Goods goods : goods) {
            if (goods.getCode() == code) {
                goods.addTotalCount(quantity);
                return;
            }
        }
        goods.add(new Goods(MyVars.config.getProductInfoByCode(code), quantity));
    }

    public boolean checkBill() {
        for (Goods goods : goods) {
            if (goods.getCurCount() != goods.getTotalCount())
                return false;
        }
        return true;
    }

    private void addMatchedGoods(Bucket bucket) {
        for (Goods goods : goods) {
            if (goods.getCode() == bucket.getCode()) {
                goods.addCurCount();
                return;
            }
        }
        goods.add(new Goods(bucket.getProductInfo(), 0));
        addMatchedGoods(bucket);
    }

    private void removeMatchedGoods(Bucket bucket) {
        for (Goods goods : goods) {
            if (goods.getCode() == bucket.getCode()) {
                goods.minusCurCount();
                break;
            }
        }
        Iterator<Goods> i = goods.iterator();
        while (i.hasNext()) {
            Goods goods = i.next();
            if (goods.getTotalCount() == 0 && goods.getCurCount() == 0) {
                i.remove();
            }
        }
    }
}