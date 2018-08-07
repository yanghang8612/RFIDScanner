package com.casc.rfidscanner.adapter;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.RegisteredCount;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

public class RegisteredCountAdapter extends BaseQuickAdapter<RegisteredCount, BaseViewHolder> {

    public RegisteredCountAdapter() {
        super(R.layout.item_registered_count, new ArrayList<RegisteredCount>());
    }

    @Override
    protected void convert(BaseViewHolder helper, RegisteredCount item) {
        helper.setText(R.id.tv_product_name, item.getProductName())
                .setText(R.id.tv_registered_count, item.getCount() + "桶");
    }

    public void addRegisteredBucket(Bucket bucket) {
        List<RegisteredCount> data = getData();
        for (int i = 0; i <= data.size(); i++) {
            if (i == data.size()) {
                data.add(new RegisteredCount(bucket.getName()));
                break;
            } else {
                if (data.get(i).getProductCode() == bucket.getCode()) {
                    data.get(i).addCount();
                    break;
                }
            }
        }
        notifyDataSetChanged();
    }
}
