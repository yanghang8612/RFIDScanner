package com.casc.rfidscanner.adapter;

import android.support.annotation.Nullable;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;

import java.util.List;

public class HintAdapter extends BaseQuickAdapter<Hint, BaseViewHolder> {

    public HintAdapter(@Nullable List<Hint> data) {
        super(R.layout.item_hint, data);
    }

    @Override
    protected void convert(BaseViewHolder helper, Hint item) {
        helper.setText(R.id.tv_hint_time, CommonUtils.convertTime(item.getTime()))
                .setText(R.id.tv_hint_content, item.getContent());
    }
}
