package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Product;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.text.SimpleDateFormat;
import java.util.List;

public class ProductAdapter extends BaseQuickAdapter<Product, BaseViewHolder> {

    public ProductAdapter(@Nullable List<Product> data) {
        super(R.layout.item_product, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Product item) {
        helper.setText(R.id.tv_product_scanned_time, new SimpleDateFormat("HH:mm:ss").format(item.getTime()))
                .setText(R.id.tv_product_body_code, item.getBodyCode())
                .setText(R.id.tv_product_spec, item.getBucketSpec())
                .setText(R.id.tv_product_name, item.getWaterBrand() + item.getWaterSpec());
    }
}
