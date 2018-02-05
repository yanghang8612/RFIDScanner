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

    private List<Product> products = new ArrayList<>();

    private GoodsAdapter goodsAdapter;

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
        this.goodsAdapter = new GoodsAdapter(goods);
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

    public List<Product> getProducts() {
        return products;
    }

    public GoodsAdapter getGoodsAdapter() {
        return goodsAdapter;
    }

    public int getDeliveryCount() {
        return products.size();
    }

    public boolean addProduct(Product product) {
        int index = findMatchedProductIndex(product);
        if (index == -1) {
            products.add(0, product);
            Goods matchedGoods = findMatchedGoods(product);
            if (matchedGoods == null) {
                matchedGoods = new Goods(
                        product.getBucketSpec(),
                        product.getWaterBrand(),
                        product.getWaterSpec(),
                        0);
                goods.add(matchedGoods);
            }
            matchedGoods.addCurCount();
        }
        goodsAdapter.notifyDataSetChanged();
        return index == -1;
    }

    public boolean removeProduct(Product product) {
        int index = findMatchedProductIndex(product);
        if (index != -1) {
            products.remove(index);
            Goods matchedGoods = findMatchedGoods(product);
            if (matchedGoods != null) {
                matchedGoods.minusCurCount();
                if (matchedGoods.getCurCount() == 0 && matchedGoods.getTotalCount() == 0)
                    goods.remove(matchedGoods);
            }
        }
        goodsAdapter.notifyDataSetChanged();
        return index != -1;
    }

    public boolean checkBill() {
        for (Goods goods : goods) {
            if (goods.getCurCount() != goods.getTotalCount())
                return false;
        }
        return true;
    }

    private int findMatchedProductIndex(Product product) {
        for (int i = 0; i < products.size(); i++) {
            if (Arrays.equals(products.get(i).getEpc(), product.getEpc())) {
                return i;
            }
        }
        return -1;
    }

    private Goods findMatchedGoods(Product product) {
        for (Goods goods : goods) {
            if (goods.isBucketMatched(product))
                return goods;
        }
        return null;
    }
}