package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Bucket;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.text.SimpleDateFormat;
import java.util.List;

public class RegisteredBucketAdapter extends BaseQuickAdapter<Bucket, BaseViewHolder> {

    public RegisteredBucketAdapter(@Nullable List<Bucket> data) {
        super(R.layout.item_registered_bucket, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Bucket item) {
        helper.setText(R.id.tv_bucket_body_code, item.getBodyCode())
                .setText(R.id.tv_bucket_spec, item.getBucketSpec() + item.getWaterBrand() + item.getWaterSpec());
    }
}
