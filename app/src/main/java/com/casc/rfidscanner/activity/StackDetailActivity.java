package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.BucketAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.NetAdapter;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.StackDeletedMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StackDetailActivity extends BaseActivity {

    private static final String TAG = StackDetailActivity.class.getSimpleName();

    private static List<String> mBucketStrs;

    private static boolean mIsStack;

    @BindView(R.id.tv_detail_stack_type) TextView mStackTypeTv;
    @BindView(R.id.tv_detail_bucket_count) TextView mBucketCountTv;
    @BindView(R.id.rv_detail_bucket_list) RecyclerView mBucketListRv;
    @BindView(R.id.btn_detail_unstack) Button mUnstackBtn;

    private List<Bucket> mBuckets =  new ArrayList<>();

    private BucketAdapter mAdapter = new BucketAdapter(mBuckets);

    public static void actionStart(Context context, List<String> buckets, boolean isStack) {
        mBucketStrs = buckets;
        mIsStack = isStack;
        if (!(ActivityCollector.getTopActivity() instanceof StackDetailActivity)) {
            Intent intent = new Intent(context, StackDetailActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    break;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stack_detail);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);

        for (String epcStr : mBucketStrs) {
            mBuckets.add(new Bucket(epcStr));
        }
        mStackTypeTv.setText(mIsStack ? "整垛" : "散货");
        mBucketCountTv.setText(mBuckets.size() + "(桶)");
        mUnstackBtn.setVisibility(mIsStack ? View.VISIBLE : View.GONE);
        mBucketListRv.setLayoutManager(new GridLayoutManager(this, 6));
        mBucketListRv.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @OnClick(R.id.btn_detail_exit)
    void onExitButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_detail_delete)
    void onDeleteButtonClicked() {
        showDialog("确认删除该垛信息吗？", new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                EventBus.getDefault().post(new StackDeletedMessage());
                finish();
            }
        });
    }

    @OnClick(R.id.btn_detail_unstack)
    void onUnstackButtonClicked() {
        showDialog("确认该整垛拆为散货吗？", new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                NetHelper.getInstance().uploadUnstackInfo(mBuckets.get(0).getBodyCode()).enqueue(new NetAdapter() {
                    @Override
                    public void onSuccess(Reply reply) {
                        EventBus.getDefault().post(new StackDeletedMessage());
                        showToast("拆垛成功");
                        finish();
                    }

                    @Override
                    public void onFail() {
                        showToast("拆垛失败,请重试");
                    }
                });
            }
        });
    }

    private void showDialog(String content, MaterialDialog.SingleButtonCallback callback) {
        new MaterialDialog.Builder(this)
                .content(content)
                .positiveText("确认")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消")
                .negativeColorRes(R.color.gray)
                .onPositive(callback)
                .show();
    }
}
