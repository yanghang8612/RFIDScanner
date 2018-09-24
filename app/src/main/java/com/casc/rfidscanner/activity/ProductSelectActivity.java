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
import com.casc.rfidscanner.bean.ProductInfo;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.ProductSelectedMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
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
        if (!(ActivityCollector.getTopActivity() instanceof ProductSelectActivity)) {
            Intent intent = new Intent(context, ProductSelectActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    @BindView(R.id.rv_product_list) RecyclerView mProductList;
    @BindView(R.id.btn_confirm_product) Button mConfirmBtn;

    private List<ProductInfo> mProducts = new ArrayList<>();

    private ProductAdapter mProductAdapter;

    private int mSelectedIndex = -1;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        mProducts.clear();
        mProducts.addAll(MyVars.config.getProductInfo());
        mProductAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_select);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        mProducts.addAll(MyVars.config.getProductInfo());
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
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @OnClick(R.id.btn_confirm_product)
    void onConfirmButtonClicked() {
        EventBus.getDefault().post(
                new ProductSelectedMessage(mProducts.get(mSelectedIndex).getName()));
        finish();
    }
}
