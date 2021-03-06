package com.casc.rfidscanner.bean;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.adapter.GoodsAdapter;
import com.casc.rfidscanner.adapter.StackAdapter;
import com.casc.rfidscanner.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
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

    private String billID;

    private String dealer;

    private String driver;

    private List<Goods> goods = new ArrayList<>();

    private List<Stack> stacks = new ArrayList<>();

    private Map<String, Long> buckets = new HashMap<>();

    private Map<String, Long> removes = new HashMap<>();

    private GoodsAdapter goodsAdapter = new GoodsAdapter(goods);

    private StackAdapter stackAdapter = new StackAdapter(stacks);

    public DeliveryBill(String billID, String dealer, String driver) {
        this.billID = billID;
        this.dealer = dealer;
        this.driver = driver;
        this.stacks.add(new Stack());
    }

    public DeliveryBill(byte[] card) {
        this(getCardIDFromEPC(card), "无", "无");
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

    public List<Goods> getGoods() {
        return goods;
    }

    public List<Stack> getStacks() {
        return stacks;
    }

    public Map<String, Long> getBuckets() {
        return buckets;
    }

    public Map<String, Long> getRemoves() {
        return removes;
    }

    public GoodsAdapter getGoodsAdapter() {
        return goodsAdapter;
    }

    public StackAdapter getStackAdapter() {
        return stackAdapter;
    }

    public void addGoods(int code, int quantity) {
        for (Goods goods : goods) {
            if (goods.getCode() == code) {
                goods.addTotalCount(quantity);
                return;
            }
        }
        goods.add(new Goods(MyVars.config.getProductByCode(code), quantity));
    }

    public void addBulk(String epcStr) {
        Stack stack = stacks.get(stacks.size() - 1);
        stack.addBucket(epcStr);
        addBucket(epcStr);
    }

    public void addStack(Stack stack) {
        if (!stacks.contains(stack)) {
            stacks.add(stacks.size() - 1, stack);
        } else {
            for (Stack s : stacks) {
                if (s.getId() == stack.getId()) {
                    s.addBuckets(stack.getBuckets());
                }
            }
        }
        for (String bucket : stack.getBuckets()) {
            addBucket(bucket);
        }
    }

    public Stack removeLastStack() {
        if (stacks.size() > 1) {
            Stack stack = stacks.get(stacks.size() - 2);
            for (String bucket : stack.getBuckets()) {
                if (buckets.containsKey(bucket)) {
                    buckets.remove(bucket);
                    removeMatchedGoods(bucket);
                }
            }
            stacks.remove(stack);
            return stack;
        }
        return null;
    }

    public boolean checkBill() {
        for (Goods goods : goods) {
            if (goods.getCurCount() != goods.getTotalCount())
                return false;
        }
        return true;
    }

    public void addBucket(String epcStr) {
        if (!buckets.containsKey(epcStr)) {
            buckets.put(epcStr, System.currentTimeMillis());
            removes.remove(epcStr);
            addMatchedGoods(epcStr);
        }
    }

    public boolean removeBucket(String epcStr) {
        if (buckets.containsKey(epcStr)) {
            buckets.remove(epcStr);
            removes.put(epcStr, System.currentTimeMillis());
            removeMatchedGoods(epcStr);
            for (Stack stack : stacks) {
                stack.removeBucket(epcStr);
            }
            return true;
        }
        return false;
    }

    private void addMatchedGoods(String epcStr) {
        IntStrPair product = CommonUtils.getProduct(epcStr);
        for (Goods goods : goods) {
            if (goods.getCode() == product.getInt()) {
                goods.addCurCount();
                return;
            }
        }
        goods.add(new Goods(product, 0));
    }

    private void removeMatchedGoods(String epcStr) {
        IntStrPair product = CommonUtils.getProduct(epcStr);
        for (Goods goods : goods) {
            if (goods.getCode() == product.getInt()) {
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