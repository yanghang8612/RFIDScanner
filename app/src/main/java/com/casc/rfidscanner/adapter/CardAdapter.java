package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Card;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.text.SimpleDateFormat;
import java.util.List;

public class CardAdapter extends BaseQuickAdapter<Card, BaseViewHolder> {

    public CardAdapter(@Nullable List<Card> data) {
        super(R.layout.item_card, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Card item) {
        helper.setText(R.id.tv_card_reg_time, new SimpleDateFormat("MM-dd HH:mm:ss").format(item.getTime()))
                .setText(R.id.tv_card_body_code, item.getBodyCode())
                .setText(R.id.tv_card_validity, item.getValidity())
                .setText(R.id.tv_card_type, item.getComment());
    }
}
