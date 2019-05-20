package com.casc.rfidscanner.adapter;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class BucketAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

    public BucketAdapter(List<String> data) {
        super(R.layout.item_bucket, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, String item) {
        helper.setText(R.id.tv_bucket_body_code, CommonUtils.getBodyCode(item))
                .setText(R.id.tv_bucket_product_name, CommonUtils.getProduct(item).getStr());
    }
}
