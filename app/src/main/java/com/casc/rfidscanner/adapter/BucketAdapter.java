package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class BucketAdapter extends BaseQuickAdapter<Bucket, BaseViewHolder> {

    public BucketAdapter(@Nullable List<Bucket> data) {
        super(R.layout.item_bucket, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Bucket item) {
        helper.setText(R.id.tv_product_scanned_time, CommonUtils.convertTime(item.getTime()))
                .setText(R.id.tv_product_body_code, item.getBodyCode())
                .setText(R.id.tv_product_name, item.getName());
    }
}
