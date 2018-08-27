package com.casc.rfidscanner.adapter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.ArrayList;

public class DeliveryBillAdapter extends BaseQuickAdapter<DeliveryBill, BaseViewHolder>  {

    private Context mContext;

    public DeliveryBillAdapter(Context context) {
        super(R.layout.item_delivery_bill, new ArrayList<DeliveryBill>());
        this.mContext = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, DeliveryBill item) {
        RecyclerView goodsListView = helper.getView(R.id.rv_delivery_bill_goods);
        // 这里导致过一个Bug，setAdapter放在外面，每次convert将会被重复设置，那么该Rv的VH复用将失效，
        // 因为每setAdapter一次都会重新刷新Rv，这与NumberSwitcher的setText产生冲突
        if (goodsListView.getLayoutManager() == null) {
            goodsListView.setLayoutManager(new LinearLayoutManager(mContext));
        }
        if (goodsListView.getAdapter() != item.getGoodsAdapter()) {
            goodsListView.setAdapter(item.getGoodsAdapter());
        }
        goodsListView.getAdapter().notifyDataSetChanged();

        ((NumberSwitcher) helper.getView(R.id.ns_delivery_count))
                .setNumber(item.getDeliveryCount());
        helper.setText(R.id.tv_delivery_bill_card_id, item.getCardID())
                .setText(R.id.tv_bill_id,
                        TextUtils.isEmpty(item.getBillID()) ? "待补单" : item.getBillID())
                .setText(R.id.tv_total_count, String.valueOf(item.getTotalCount()))
                .setGone(R.id.btn_state_delivery, !item.isBacking())
                .setGone(R.id.btn_state_back, item.isBacking())
                .addOnClickListener(R.id.btn_state_delivery)
                .addOnClickListener(R.id.btn_state_back)
                .addOnClickListener(R.id.btn_confirm_delivery);
    }

    public void showBill(DeliveryBill bill) {
        getData().clear();
        if (bill != null) getData().add(bill);
        notifyDataSetChanged();
    }
}
