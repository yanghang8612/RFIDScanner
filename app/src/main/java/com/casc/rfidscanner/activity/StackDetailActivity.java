package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.BucketAdapter;
import com.casc.rfidscanner.message.StackDeletedMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;

public class StackDetailActivity extends BaseActivity {

    private static final String TAG = StackDetailActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        if (ActivityCollector.topNotOf(StackDetailActivity.class)) {
            Intent intent = new Intent(context, StackDetailActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    private List<String> mBuckets =  new ArrayList<>();

    private BucketAdapter mAdapter = new BucketAdapter(mBuckets);

    @BindView(R.id.tv_detail_stack_type) TextView mStackTypeTv;
    @BindView(R.id.tv_detail_bucket_count) TextView mBucketCountTv;
    @BindView(R.id.rv_detail_bucket_list) RecyclerView mBucketListRv;

    @OnClick(R.id.btn_detail_exit) void onExitButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_detail_delete) void onDeleteButtonClicked() {
        showDialog("确认删除该垛信息吗？", new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                EventBus.getDefault().post(new StackDeletedMessage());
                finish();
            }
        });
    }

    @Override
    protected void initActivity() {
        mBuckets.addAll(MyVars.stackToShow.getBuckets());
        mStackTypeTv.setText(MyVars.stackToShow.isBulk() ? "散货" : "整垛");
        mBucketCountTv.setText(mBuckets.size() + "(桶)");
        mBucketListRv.setLayoutManager(new GridLayoutManager(this, 6));
        mBucketListRv.setAdapter(mAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.activity_stack_detail;
    }

}
