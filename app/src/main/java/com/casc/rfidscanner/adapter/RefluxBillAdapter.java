package com.casc.rfidscanner.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.RefluxBill;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class RefluxBillAdapter extends BaseQuickAdapter<RefluxBill, BaseViewHolder> implements BaseQuickAdapter.OnItemClickListener {

    // Adapter上下文，提供给创建goods列表view的LayoutManager使用
    private Context context;

    public RefluxBillAdapter(Context context, @Nullable List<RefluxBill> data) {
        super(R.layout.item_reflux_bill, data);
        this.context = context;
    }

    @Override
    protected void convert(BaseViewHolder helper, RefluxBill item) {
        CardView root = helper.getView(R.id.cv_reflux_bill_root);
        root.setCardBackgroundColor(item.isHighlight() ?
                MyApplication.getInstance().getResources().getColor(R.color.bright_gray) :
                MyApplication.getInstance().getResources().getColor(R.color.snow));
        item.getGoodsAdapter().setOnItemClickListener(this);
        RecyclerView goodsListView = helper.getView(R.id.rv_reflux_bill_goods);
        if (goodsListView.getLayoutManager() == null)
            goodsListView.setLayoutManager(new LinearLayoutManager(context));
        goodsListView.setAdapter(item.getGoodsAdapter());
        helper.setText(R.id.tv_reflux_bill_card_id, item.getCardID())
                .setText(R.id.tv_reflux_count, String.valueOf(item.getRefluxCount()))
                .addOnClickListener(R.id.btn_reflux_bill_detail);
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        while (!(view instanceof CardView)) {
            view = (View) view.getParent();
        }
        view.callOnClick();
    }
}
