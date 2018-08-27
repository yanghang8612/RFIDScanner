package com.casc.rfidscanner.adapter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.ArrayList;

public class RefluxBillAdapter extends BaseQuickAdapter<RefluxBill, BaseViewHolder> {

    private Context mContext;

    public RefluxBillAdapter(Context context) {
        super(R.layout.item_reflux_bill, new ArrayList<RefluxBill>());
        this.mContext = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, RefluxBill item) {
        RecyclerView goodsListView = helper.getView(R.id.rv_reflux_bill_goods);
        if (goodsListView.getLayoutManager() == null) {
            goodsListView.setLayoutManager(new LinearLayoutManager(mContext));
        }
        if (goodsListView.getAdapter() != item.getGoodsAdapter()) {
            goodsListView.setAdapter(item.getGoodsAdapter());
        }
        goodsListView.getAdapter().notifyDataSetChanged();

        ((NumberSwitcher) helper.getView(R.id.ns_reflux_count))
                .setNumber(item.getRefluxCount());
        helper.setText(R.id.tv_reflux_bill_card_id, item.getCardID())
                .addOnClickListener(R.id.btn_confirm_reflux);
    }

    public void showBill(RefluxBill bill) {
        getData().clear();
        if (bill != null) getData().add(bill);
        notifyDataSetChanged();
    }
}
