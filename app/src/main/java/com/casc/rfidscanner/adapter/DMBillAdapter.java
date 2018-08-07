package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.text.TextUtils;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class DMBillAdapter extends BaseQuickAdapter<DeliveryBill, BaseViewHolder>  {

    public DMBillAdapter(@Nullable List<DeliveryBill> data) {
        super(R.layout.item_dm_bill, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, DeliveryBill item) {
//        CardView root = helper.getView(R.id.cv_dm_bill_root);
//        root.setCardBackgroundColor(item.isHighlight() ?
//                MyApplication.getInstance().getColor(R.color.powder_blue) :
//                item.checkGoods() ? MyApplication.getInstance().getColor(R.color.snow) :
//                        MyApplication.getInstance().getColor(R.color.indian_red));
        helper.setBackgroundRes(R.id.tv_bill_status,
                item.isComplete() ? R.drawable.bg_indian_red :
                        R.drawable.bg_light_grey);
        helper.setBackgroundRes(R.id.tv_dm_bill_card_id,
                item.checkGoods() ? R.drawable.bg_bill_card_normal :
                        R.drawable.bg_bill_card_abnormal);
        ((NumberSwitcher) helper.getView(R.id.ns_dm_delivery_count))
                .setNumber(item.getDeliveryCount());
        helper.setText(R.id.tv_bill_status, item.isComplete() ? "已完成" : "出库中")
                .setText(R.id.tv_dm_bill_card_id, item.getCardID())
                .setText(R.id.tv_dm_bill_id,
                        TextUtils.isEmpty(item.getBillID()) ? "待补单" : item.getBillID())
                .setText(R.id.tv_dm_total_count, String.valueOf(item.getTotalCount()));
    }
}
