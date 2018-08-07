package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Client;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class ClientAdapter extends BaseQuickAdapter<Client, BaseViewHolder> {

    private Animation alphaAnimation;

    public ClientAdapter(@Nullable List<Client> data) {
        super(R.layout.item_client, data);
        this.alphaAnimation = new AlphaAnimation(1, 0);
        this.alphaAnimation.setDuration(15);
        this.alphaAnimation.setInterpolator(new LinearInterpolator());
        this.alphaAnimation.setRepeatCount(0);
    }

    @Override
    protected void convert(BaseViewHolder helper, Client item) {
        helper.setText(R.id.tv_client_name, item.getName())
                .setImageResource(R.id.iv_client_icon, item.isOnline() ?
                        R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal)
                .setText(R.id.tv_client_status, item.isOnline() ? "在线" : "离线");
        if (item.isOnline() && item.isDataIncoming()) {
            synchronized (item) {
                item.setDataIncoming(false);
                helper.getView(R.id.iv_client_icon).startAnimation(alphaAnimation);
            }
        }
    }
}
