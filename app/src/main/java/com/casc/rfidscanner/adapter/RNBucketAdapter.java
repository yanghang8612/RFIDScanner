package com.casc.rfidscanner.adapter;

import android.support.v7.widget.CardView;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.RNBucket;
import com.chad.library.adapter.base.BaseItemDraggableAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class RNBucketAdapter extends BaseItemDraggableAdapter<RNBucket, BaseViewHolder> {

    public RNBucketAdapter(List<RNBucket> data) {
        super(R.layout.item_rn_bucket, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, RNBucket item) {
        CardView root = helper.getView(R.id.cv_rn_bucket_root);
        root.setCardBackgroundColor(item.isHighlight() ?
                MyApplication.getInstance().getColor(R.color.powder_blue) :
                MyApplication.getInstance().getColor(R.color.snow));
        helper.setText(R.id.tv_rn_bucket_body_code, item.getBodyCode());
    }
}
