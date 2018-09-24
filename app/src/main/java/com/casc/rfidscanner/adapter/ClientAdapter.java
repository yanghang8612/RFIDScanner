package com.casc.rfidscanner.adapter;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.DeliveryDetailActivity;
import com.casc.rfidscanner.bean.Client;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class ClientAdapter extends BaseQuickAdapter<Client, BaseViewHolder> {

    private Context mContext;

    private Animation alphaAnimation;

    public ClientAdapter(Context context, @Nullable List<Client> data) {
        super(R.layout.item_client, data);
        this.mContext = context;
        this.alphaAnimation = new AlphaAnimation(1, 0);
        this.alphaAnimation.setDuration(15);
        this.alphaAnimation.setInterpolator(new LinearInterpolator());
        this.alphaAnimation.setRepeatCount(0);
    }

    @Override
    protected void convert(BaseViewHolder helper, final Client item) {
        helper.setText(R.id.tv_client_name, item.getName())
                .setImageResource(R.id.iv_client_icon, item.isOnline() ?
                        R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal)
                .setText(R.id.tv_client_status, item.isOnline() ? "在线" : "离线")
                .setText(R.id.tv_client_bill_count, String.valueOf(item.getCompleteCount()));
        RecyclerView billList = helper.getView(R.id.rv_client_bill_list);
        if (billList.getLayoutManager() == null) {
            billList.setLayoutManager(
                    new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
            item.getAdapter().setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                    MyVars.deliveryBillToShow = item.getBills().get(position);
                    DeliveryDetailActivity.actionStart(mContext);
                }
            });
            billList.setAdapter(item.getAdapter());
        }
        if (item.isOnline() && item.isDataIncoming()) {
            synchronized (item) {
                item.getAdapter().notifyDataSetChanged();
                item.setDataIncoming(false);
                helper.getView(R.id.iv_client_icon).startAnimation(alphaAnimation);
            }
        }
    }
}
