package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Goods;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class GoodsAdapter extends BaseQuickAdapter<Goods, BaseViewHolder> {

    public GoodsAdapter(@Nullable List<Goods> data) {
        super(R.layout.item_goods, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Goods item) {
        if (item.getTotalCount() == 0) {
            helper.setVisible(R.id.tv_goods_total_count, false);
        }
        if (item.getTotalCount() == -1) {
            helper.setGone(R.id.tv_goods_total_count, false);
        } else {
            helper.setTextColor(R.id.tv_goods_cur_count,
                    item.getCurCount() > item.getTotalCount() ?
                            MyApplication.getInstance().getColor(R.color.indian_red) :
                            MyApplication.getInstance().getColor(R.color.black));
//            helper.setBackgroundRes(R.id.tv_goods_cur_count, item.getCurCount() > item.getTotalCount() ?
//                    R.drawable.bg_count_over : R.drawable.bg_count_normal);
        }
        helper.setText(R.id.tv_goods_spec, item.getBucketSpec())
                .setText(R.id.tv_goods_name, item.getWaterBrand() + item.getWaterSpec())
                .setText(R.id.tv_goods_cur_count, String.valueOf(item.getCurCount()))
                .setText(R.id.tv_goods_total_count, String.valueOf(item.getTotalCount()));
    }
}
