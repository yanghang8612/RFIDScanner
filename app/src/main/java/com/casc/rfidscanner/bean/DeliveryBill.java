package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.adapter.GoodsAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class DeliveryBill {

    private boolean isHighlight;

    private long updatedTime;

    private byte[] cardEPC;

    private int dayOfYear, computerNo, billNo;

    private String cardID;

    private String billID;

    private int totalCount;

    private List<Goods> goods = new ArrayList<>();

    private List<Bucket> buckets = new ArrayList<>();

    public DeliveryBill(byte[] cardEPC) {
        this.cardEPC = cardEPC;
        this.cardID = MyVars.config.getCompanySymbol() + "C" + String.format("%03d", cardEPC[6] & 0xFF);
        this.dayOfYear = ((cardEPC[7] & 0xFF) << 1) + ((cardEPC[8] & 0xFF) >> 7);
        this.computerNo = (cardEPC[8] & 0x7F) >> 1;
        this.billNo = ((cardEPC[8] & 0x01) << 7) + ((cardEPC[9] & 0xFF) >> 1);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        this.billID = MyVars.config.getCompanySymbol() +
                        String.format("%02d", computerNo) + "1" +
                        new SimpleDateFormat("yyMMdd").format(calendar.getTime()) +
                        String.format("%03d", billNo);
        for (int i = 0; i < 5; i++) {
            if (cardEPC[13 + i * 4] != 0) {
                goods.add(new Goods(
                        MyVars.config.getBucketSpecByCode(cardEPC[10 + i * 4]),
                        MyVars.config.getWaterBrandByCode(cardEPC[12 + i * 4]),
                        MyVars.config.getWaterSpecByCode(cardEPC[11 + i * 4]),
                        cardEPC[13 + i * 4] & 0xFF));
                totalCount += cardEPC[13 + i * 4] & 0xFF;
                if (i == 4) {
                    Goods gift = goods.get(goods.size() - 1);
                    for (int j = 0; j < goods.size() - 1; j++) {
                        if (goods.get(j).equals(gift)) {
                            goods.remove(goods.size() - 1);
                            break;
                        }
                    }
                }
            }
        }
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

    public int getDayOfYear() {
        return dayOfYear;
    }

    public int getComputerNo() {
        return computerNo;
    }

    public int getBillNo() {
        return billNo;
    }

    public String getCardID() {
        return cardID;
    }

    public String getCardNum() {
        return cardID.substring(3) + "号";
    }

    public String getBillID() {
        return billID;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public List<Goods> getGoods() {
        return goods;
    }

    public List<Bucket> getBuckets() {
        return buckets;
    }

    public int getDeliveryCount() {
        return buckets.size();
    }

    public boolean addProduct(Bucket bucket) {
        int index = findMatchedProductIndex(bucket);
        if (index == -1) {
            buckets.add(0, bucket);
            Goods matchedGoods = findMatchedGoods(bucket);
            if (matchedGoods == null) {
                matchedGoods = new Goods(
                        bucket.getBucketSpec(),
                        bucket.getWaterBrand(),
                        bucket.getWaterSpec(),
                        0);
                goods.add(matchedGoods);
            }
            matchedGoods.addCurCount();
        }
        return index == -1;
    }

    public boolean removeProduct(Bucket bucket) {
        int index = findMatchedProductIndex(bucket);
        if (index != -1) {
            buckets.remove(index);
            Goods matchedGoods = findMatchedGoods(bucket);
            if (matchedGoods != null) {
                matchedGoods.minusCurCount();
                if (matchedGoods.getCurCount() == 0 && matchedGoods.getTotalCount() == 0)
                    goods.remove(matchedGoods);
            }
        }
        return index != -1;
    }

    public boolean checkBill() {
        for (Goods goods : goods) {
            if (goods.getCurCount() != goods.getTotalCount())
                return false;
        }
        return true;
    }

    public boolean checkGoods() {
        for (Goods goods : goods) {
            if (goods.getTotalCount() == 0)
                return false;
        }
        return buckets.size() <= totalCount;
    }

    private int findMatchedProductIndex(Bucket bucket) {
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