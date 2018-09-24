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

    private enum BillState {

        IS_STACK32("3"), IS_STACK48("4"), IS_SINGLE("1"), IS_BACK("2");

        final String flag;

        BillState(String flag) {
            this.flag = flag;
        }

        public static BillState getStateByFlag(String flag) {
            for (BillState state : BillState.values()) {
                if (state.flag.equals(flag))
                    return state;
            }
            throw new IllegalArgumentException("No matched state type");
        }
    }

    private BillState state = BillState.IS_STACK32;

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

    public DeliveryBill(String card) {
        this(CommonUtils.hexToBytes(card));
    }

    public DeliveryBill(byte[] card) {
        // 解析出库专用卡EPC
        this.card = card;
        int cardNo = 0;
        cardNo += (card[5] & 0xFF) << 8;
        cardNo += (card[6] & 0xFF);
        this.cardID = MyVars.config.getCompanySymbol() + "C" + String.format("%03d", cardNo);
        this.goodsAdapter = new GoodsAdapter(goods, true);
    }

    public DeliveryBill(String card, String bill) {
        this(CommonUtils.hexToBytes(card), CommonUtils.hexToBytes(bill));
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

    public boolean isStack32() {
        return state == BillState.IS_STACK32;
    }

    public void setStack32() {
        state = BillState.IS_STACK32;
    }

    public boolean isStack48() {
        return state == BillState.IS_STACK48;
    }

    public void setStack48() {
        state = BillState.IS_STACK48;
    }

    public boolean isSingle() {
        return state == BillState.IS_SINGLE;
    }

    public void setSingle() {
        state = BillState.IS_SINGLE;
    }

    public boolean isBack() {
        return state == BillState.IS_BACK;
    }

    public void setBack() {
        state = BillState.IS_BACK;
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
        int deliveryCount = 0;
        for (Goods goods : goods) {
            deliveryCount += goods.getCurCount();
        }
        return deliveryCount;
    }

    public Bucket addBucket(byte[] epc) {
        String epcStr = CommonUtils.bytesToHex(epc);
        if (state == BillState.IS_STACK32 || state == BillState.IS_STACK48) {
            String key1 = BillState.IS_STACK32.flag + epcStr;
            String key2 = BillState.IS_STACK48.flag + epcStr;
            if (!buckets.containsKey(key1) && !buckets.containsKey(key2)) {
                Bucket bucket = new Bucket(epc, state.flag);
                buckets.put(state.flag + epcStr, bucket);
                addMatchedGoods(bucket);
                return bucket;
            }

        } else {
            String key = state.flag + epcStr;
            if (!buckets.containsKey(key)) {
                Bucket bucket = new Bucket(epc, state.flag);
                buckets.put(key, bucket);
                addMatchedGoods(bucket);
                return bucket;
            }
        }
        return null;
    }

    public boolean addBucket(Bucket bucket) {
        if (!buckets.containsKey(bucket.getKey())) {
            buckets.put(bucket.getKey(), bucket);
            addMatchedGoods(bucket);
            return true;
        }
        return false;
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
        return getDeliveryCount() <= totalCount;
    }

    private void addMatchedGoods(Bucket bucket) {
        for (Goods goods : goods) {
            if (goods.getCode() == bucket.getCode()) {
                switch (BillState.getStateByFlag(bucket.getFlag())) {
                    case IS_STACK32:
                        goods.addStack32();
                        break;
                    case IS_STACK48:
                        goods.addStack48();
                        break;
                    case IS_SINGLE:
                        goods.addSingle();
                        break;
                    case IS_BACK:
                        goods.addBack();
                        break;
                }
                return;
            }
        }
        goods.add(new Goods(bucket.getProductInfo(), 0));
        addMatchedGoods(bucket);
    }
}