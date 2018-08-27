package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.ProductInfo;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class ProductAdapter extends BaseQuickAdapter<ProductInfo, BaseViewHolder> {

    public ProductAdapter(@Nullable List<ProductInfo> data) {
        super(R.layout.item_product, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, ProductInfo item) {
        helper.setText(R.id.tv_product_name, item.getName());
    }
}
