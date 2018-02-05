package com.casc.rfidscanner.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class DeliveryBillAdapter extends BaseQuickAdapter<DeliveryBill, BaseViewHolder> implements BaseQuickAdapter.OnItemClickListener {

    // Adapter上下文，提供给创建goods列表view的LayoutManager使用
    private Context context;

    public DeliveryBillAdapter(Context context, @Nullable List<DeliveryBill> data) {
        super(R.layout.item_delivery_bill, data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, DeliveryBill item) {
        CardView root = helper.getView(R.id.cv_deliver_bill_root);
        root.setCardBackgroundColor(item.isHighlight() ?
                context.getResources().getColor(R.color.bright_gray) :
                context.getResources().getColor(R.color.snow));
        item.getGoodsAdapter().setOnItemClickListener(this);
        RecyclerView goodsListView = helper.getView(R.id.rv_delivery_bill_goods);
        if (goodsListView.getLayoutManager() == null)
            goodsListView.setLayoutManager(new LinearLayoutManager(context));
        goodsListView.setAdapter(item.getGoodsAdapter());
        item.getGoodsAdapter().notifyDataSetChanged();
        helper.setText(R.id.tv_delivery_bill_card_id, item.getCardID())
                .setText(R.id.tv_bill_id, item.getBillID())
                .setText(R.id.tv_total_count, String.valueOf(item.getTotalCount()))
                .setText(R.id.tv_delivery_count, String.valueOf(item.getDeliveryCount()))
                .addOnClickListener(R.id.rv_delivery_bill_goods);
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        while (!(view instanceof CardView)) {
            view = (View) view.getParent();
        }
        view.callOnClick();
    }
}
