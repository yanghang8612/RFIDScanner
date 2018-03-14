package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.RefluxBill;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class RefluxBillAdapter extends BaseQuickAdapter<RefluxBill, BaseViewHolder> {

    public RefluxBillAdapter(@Nullable List<RefluxBill> data) {
        super(R.layout.item_reflux_bill, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, RefluxBill item) {
        CardView root = helper.getView(R.id.cv_reflux_bill_root);
        root.setCardBackgroundColor(item.isHighlight() ?
                MyApplication.getInstance().getColor(R.color.powder_blue) :
                MyApplication.getInstance().getColor(R.color.snow));
        helper.setBackgroundRes(R.id.tv_reflux_bill_card_id,
                item.isHighlight() ? R.drawable.bg_bill_card_highlight :
                        R.drawable.bg_bill_card_normal);
        helper.setText(R.id.tv_reflux_bill_card_id, item.getCardID())
                .setText(R.id.tv_reflux_count, String.valueOf(item.getRefluxCount()));
    }
}
