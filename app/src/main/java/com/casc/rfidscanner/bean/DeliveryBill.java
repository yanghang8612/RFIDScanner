package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.adapter.GoodsAdapter;
import com.casc.rfidscanner.utils.CommonUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private boolean isBacking, isComplete;

    private long updatedTime = System.currentTimeMillis();

    private byte[] card;

    private byte[] bill;

    private int dayOfYear, computerNo, billNo;

    private String cardID;

    private String billID;

    private int totalCount;

    private List<Goods> goods = new ArrayList<>();

    private Map<String, Bucket> buckets = new LinkedHashMap<>();

    private GoodsAdapter goodsAdapter;

    public DeliveryBill(byte[] card) {
        // 解析出库专用卡EPC
        this.card = card;
        int cardNo = 0;
        cardNo += (card[5] & 0xFF) << 8;
        cardNo += (card[6] & 0xFF);
        this.cardID = MyVars.config.getCompanySymbol() + "C" + String.format("%03d", cardNo);
        this.goodsAdapter = new GoodsAdapter(goods, true);
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

    public boolean isBacking() {
        return isBacking;
    }

    public void setBacking(boolean backing) {
        isBacking = backing;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
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

    public byte[] getBill() {
        return bill;
    }

    public String getBillStr() {
        return CommonUtils.bytesToHex(bill);
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
        return new ArrayList<>(buckets.values());
    }

    public GoodsAdapter getGoodsAdapter() {
        return goodsAdapter;
    }

    public int getDeliveryCount() {
        return buckets.size();
    }

    public boolean addBucket(String bucketEPC) {
        return addBucket(new Bucket(bucketEPC));
    }

    public boolean addBucket(Bucket bucket) {
        if (!buckets.containsKey(bucket.getEpcStr())) {
            buckets.put(bucket.getEpcStr(), bucket);
            Goods matchedGoods = findMatchedGoods(bucket);
            if (matchedGoods == null) {
                matchedGoods = new Goods(bucket.getProductInfo(), 0);
                goods.add(matchedGoods);
            }
            matchedGoods.addCurCount();
            return true;
        }
        return false;
    }

    public boolean removeBucket(String bucketEPC) {
        if (buckets.containsKey(bucketEPC)) {
            Bucket bucket = buckets.remove(bucketEPC);
            Goods matchedGoods = findMatchedGoods(bucket);
            if (matchedGoods != null) {
                matchedGoods.minusCurCount();
                if (matchedGoods.getCurCount() == 0 && matchedGoods.getTotalCount() == 0)
                    goods.remove(matchedGoods);
            }
            return true;
        }
        return false;
    }

    public boolean removeBucket(Bucket bucket) {
        return removeBucket(bucket.getEpcStr());
    }

    public boolean checkBill() {
        for (Goods goods : goods) {
            if (goods.getCurCount() != goods.getTotalCount())
                return false;
        }
        return true;
    }

    public boolean checkGoods() {
        if (bill == null) return true; // 补单的出库单，检查始终合法
        for (Goods goods : goods) {
            if (goods.getTotalCount() == 0)
                return false;
        }
        return buckets.size() <= totalCount;
    }

    private Goods findMatchedGoods(Bucket bucket) {
        for (Goods goods : goods) {
            if (goods.isBucketMatched(bucket))
                return goods;
        }
        return null;
    }
}