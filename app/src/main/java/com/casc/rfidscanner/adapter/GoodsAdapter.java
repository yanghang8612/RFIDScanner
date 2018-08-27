package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;
import android.view.View;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Goods;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class GoodsAdapter extends BaseQuickAdapter<Goods, BaseViewHolder> {

    private boolean isShowLeftCount;

    public GoodsAdapter(@Nullable List<Goods> data) {
        super(R.layout.item_goods, data);
    }

    public GoodsAdapter(@Nullable List<Goods> data, boolean isShowLeftCount) {
        this(data);
        this.isShowLeftCount = isShowLeftCount;
    }

    @Override
    protected void convert(BaseViewHolder helper, Goods item) {
        ((NumberSwitcher) helper.getView(R.id.ns_goods_left_count))
                .setNumber(item.getLeftCount());
        ((NumberSwitcher) helper.getView(R.id.ns_goods_cur_count))
                .setNumber(item.getCurCount());
        ((NumberSwitcher) helper.getView(R.id.ns_goods_cur_count))
                .setTextColor(item.getCurCount() > item.getTotalCount() && item.getTotalCount() != -1 ?
                        MyApplication.getInstance().getColor(R.color.indian_red) :
                        MyApplication.getInstance().getColor(R.color.black));
        helper.setText(R.id.tv_goods_name, item.getName())
                .setText(R.id.tv_goods_total_count, String.valueOf(item.getTotalCount()))
                .setGone(R.id.ns_goods_left_count, isShowLeftCount);
        helper.getView(R.id.tv_goods_total_count).setVisibility(
                item.getTotalCount() == -1 ? View.GONE :
                        item.getTotalCount() == 0 ? View.INVISIBLE : View.VISIBLE);
    }
}
