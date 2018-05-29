package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.text.TextUtils;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class DeliveryBillAdapter extends BaseQuickAdapter<DeliveryBill, BaseViewHolder>  {

    public DeliveryBillAdapter(@Nullable List<DeliveryBill> data) {
        super(R.layout.item_delivery_bill, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DeliveryBill item) {
        CardView root = helper.getView(R.id.cv_deliver_bill_root);
        root.setCardBackgroundColor(item.isHighlight() ?
                MyApplication.getInstance().getColor(R.color.powder_blue) :
                item.checkGoods() ? MyApplication.getInstance().getColor(R.color.snow) :
                        MyApplication.getInstance().getColor(R.color.indian_red));
        helper.setBackgroundRes(R.id.tv_delivery_bill_card_id,
                item.isHighlight() ? R.drawable.bg_bill_card_highlight :
                        R.drawable.bg_bill_card_normal);
        helper.setText(R.id.tv_delivery_bill_card_id, item.getCardID())
                .setText(R.id.tv_bill_id, TextUtils.isEmpty(item.getBillID()) ? "待补单" : item.getBillID())
                .setText(R.id.tv_total_count, String.valueOf(item.getTotalCount()))
                .setText(R.id.tv_delivery_count, String.valueOf(item.getDeliveryCount()));
    }
}
