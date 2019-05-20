package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.IntStrPair;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class ProductAdapter extends BaseQuickAdapter<IntStrPair, BaseViewHolder> {

    public ProductAdapter(@Nullable List<IntStrPair> data) {
        super(R.layout.item_product, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, IntStrPair item) {
        helper.setText(R.id.tv_product_name, item.getStr());
    }
}
