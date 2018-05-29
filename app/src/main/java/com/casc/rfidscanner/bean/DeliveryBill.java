package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class DeliveryBill {

    private static final int DAYOFYEAR_LENGTH = 9;
    private static final int DAYOFYEAR_INDEX = 0;

    private static final int COMPUTERNO_LENGTH = 6;
    private static final int COMPUTERNO_INDEX = 9;

    private static final int BILLNO_LENGTH = 10;
    private static final int BILLNO_INDEX = 9 + 6;

    private static final int SPECCOUNT_LENGTH = 4;
    private static final int SPECCOUNT_INDEX = 9 + 6 + 10;

    private static final int SPEC_LENGTH = 20;
    private static final int SPEC_INDEX = 9 + 6 + 10 + 4;

    private boolean isHighlight;

    private long updatedTime = System.currentTimeMillis();

    private byte[] card;

    private byte[] bill;

    private int dayOfYear, computerNo, billNo;

    private String cardID;

    private String billID;

    private int totalCount;

    private List<Goods> goods = new ArrayList<>();

    private List<Bucket> buckets = new ArrayList<>();

    public DeliveryBill(byte[] card) {
        // 解析出库专用卡EPC
        this.card = card;
        int cardNo = 0;
        cardNo += (card[5] & 0xFF) << 8;
        cardNo += (card[6] & 0xFF);
        this.cardID = MyVars.config.getCompanySymbol() + "C" + String.format("%03d", cardNo);
    }

    public DeliveryBill(byte[] card, byte[] bill) {
        this(card);
        // 解析出库专用卡User Memory中的电子提货单
        // 首先解析 年积日 电脑序号 票据序号
        this.bill = bill;
        this.dayOfYear = (int) CommonUtils.getBitsFromBytes(bill, DAYOFYEAR_INDEX, DAYOFYEAR_LENGTH);
        this.computerNo = (int) CommonUtils.getBitsFromBytes(bill, COMPUTERNO_INDEX, COMPUTERNO_LENGTH);
        this.billNo = (int) CommonUtils.getBitsFromBytes(bill, BILLNO_INDEX, BILLNO_LENGTH);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_YEAR, dayOfYear);
        this.billID = MyVars.config.getCompanySymbol() +
                        String.format("%02d", computerNo) + "1" +
                        new SimpleDateFormat("yyMMdd").format(calendar.getTime()) +
                        String.format("%03d", billNo);

        // 再解析 总规格数量 各规格详情
        int specCount = (int) CommonUtils.getBitsFromBytes(bill, SPECCOUNT_INDEX, SPECCOUNT_LENGTH);
        for (int i = 0; i < specCount; i++) {
            int part = (int) CommonUtils.getBitsFromBytes(bill, SPEC_INDEX + i * SPEC_LENGTH, SPEC_LENGTH);
            int code = part >> 12;
            int count = part & 0xFFF;
            for (int j = 0; j <= goods.size(); j++) {
                if (j == goods.size()) {
                    goods.add(new Goods(MyVars.config.getProductInfoByCode(part >> 12), count));
                    break;
                }
                else if (code == goods.get(j).getCode()) {
                    goods.get(j).addTotalCount(count);
                    break;
                }
            }
            totalCount += count;
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

    public byte[] getCard() {
        return card;
    }

    public byte[] getBill() {
        return bill;
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
                matchedGoods = new Goods(bucket.getProductInfo(), 0);
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