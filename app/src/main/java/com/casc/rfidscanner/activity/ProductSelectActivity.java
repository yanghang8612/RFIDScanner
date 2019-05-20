package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.ProductAdapter;
import com.casc.rfidscanner.bean.IntStrPair;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.ProductSelectedMessage;
import com.chad.library.adapter.base.BaseQuickAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ProductSelectActivity extends BaseActivity {

    private static final String TAG = ProductSelectActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        if (ActivityCollector.topNotOf(ProductSelectActivity.class)) {
            Intent intent = new Intent(context, ProductSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    private int mSelectedIndex = -1;

    private ProductAdapter mProductAdapter;

    private List<IntStrPair> mProducts = new ArrayList<>();

    @BindView(R.id.rv_product_list) RecyclerView mProductList;
    @BindView(R.id.btn_confirm_product) Button mConfirmBtn;

    @OnClick(R.id.btn_confirm_product) void onConfirmButtonClicked() {
        EventBus.getDefault().post(
                new ProductSelectedMessage(mProducts.get(mSelectedIndex).getStr()));
        finish();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        mProducts.clear();
        mProducts.addAll(MyVars.config.getProducts());
        mProductAdapter.notifyDataSetChanged();
    }

    @Override
    protected void initActivity() {
        EventBus.getDefault().register(this);

        mProducts.addAll(MyVars.config.getProducts());
        mProductAdapter = new ProductAdapter(mProducts);
        mProductAdapter.bindToRecyclerView(mProductList);
        mProductAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                mConfirmBtn.setEnabled(true);
                View preView = mSelectedIndex == -1 ? null
                        : adapter.getViewByPosition(mSelectedIndex, R.id.ll_product_content);
                if (preView != null)
                    preView.setBackground(getDrawable(R.drawable.bg_reader_normal));
                if (view != null)
                    view.findViewById(R.id.ll_product_content).setBackground(getDrawable(R.drawable.bg_reader_selected));
                mSelectedIndex = position;
            }
        });
        mProductList.setLayoutManager(new GridLayoutManager(this, 2));
        mProductList.setAdapter(mProductAdapter);
        mProductAdapter.notifyDataSetChanged();
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_product_select;
    }

}
