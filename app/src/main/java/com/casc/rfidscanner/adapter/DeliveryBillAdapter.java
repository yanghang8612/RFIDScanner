package com.casc.rfidscanner.adapter;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewParent;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class DeliveryBillAdapter extends BaseQuickAdapter<DeliveryBill, BaseViewHolder> implements BaseQuickAdapter.OnItemClickListener {

    private Context mContext;

    public DeliveryBillAdapter(Context context, List<DeliveryBill> data) {
        super(R.layout.item_delivery_bill, data);
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
            item.getGoodsAdapter().setOnItemClickListener(this);
            goodsListView.setAdapter(item.getGoodsAdapter());
        }
        goodsListView.getAdapter().notifyDataSetChanged();

        helper.setText(R.id.tv_bill_id, item.getBillID())
                .setText(R.id.tv_bill_dealer, item.getDealer())
                .setText(R.id.tv_bill_driver, item.getDriver())
                .addOnClickListener(R.id.btn_delivery_bill_cancel);

        RecyclerView stackListView = helper.getView(R.id.rv_delivery_bill_stacks);
        if (stackListView.getLayoutManager() == null) {
            stackListView.setLayoutManager(new GridLayoutManager(mContext, 2));
        }
        if (stackListView.getAdapter() != item.getStackAdapter()) {
            item.getStackAdapter().setOnItemClickListener(this);
            stackListView.setAdapter(item.getStackAdapter());
        }
        stackListView.getAdapter().notifyDataSetChanged();
    }

    public void moveToFirst(DeliveryBill bill) {
        if (bill != null) {
            getData().remove(bill);
            getData().add(0, bill);
        }
        notifyDataSetChanged();
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        ViewParent parent = view.getParent();
        while (!(parent instanceof CardView)) {
            parent = parent.getParent();
        }
        ((CardView) parent).callOnClick();
    }
}
