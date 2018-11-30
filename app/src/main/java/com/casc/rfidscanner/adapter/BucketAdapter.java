package com.casc.rfidscanner.adapter;

import android.support.v7.widget.CardView;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Bucket;
import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class BucketAdapter extends BaseItemDraggableAdapter<Bucket, BaseViewHolder> {

    public BucketAdapter(List<Bucket> data) {
        super(R.layout.item_bucket, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Bucket item) {
        CardView root = helper.getView(R.id.cv_rn_bucket_root);
        root.setCardBackgroundColor(item.isHighlight() ?
                MyApplication.getInstance().getColor(R.color.powder_blue) :
                MyApplication.getInstance().getColor(R.color.snow));
        helper.setText(R.id.tv_bucket_body_code, item.getBodyCode())
                .setText(R.id.tv_bucket_product_name, item.getProductInfo().getName());
    }
}
