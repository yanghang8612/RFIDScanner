package com.casc.rfidscanner.fragment;

import android.graphics.Canvas;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.RNBucketAdapter;
import com.casc.rfidscanner.bean.RNBucket;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageDealer;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback;
import com.chad.library.adapter.base.listener.OnItemSwipeListener;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 水站库存管理Fragment
 */

public class RNFragment extends BaseFragment {

    private static final String TAG = RNFragment.class.getSimpleName();

    @BindView(R.id.spn_rn_link) BetterSpinner mRNLinkSpn;
    @BindView(R.id.act_rn_driver) AutoCompleteTextView mRNDriverAct;
    @BindView(R.id.tv_rn_counter_label) TextView mRNCounterLabelTv;
    @BindView(R.id.act_rn_counter) AutoCompleteTextView mRNCounterAct;
    @BindView(R.id.rv_rn_buckets) RecyclerView mBucketsRv;
    @BindView(R.id.tv_rn_bucket_count) TextView mBucketCountTv;

    @BindView(R.id.tv_rn_uploaded_bill_count) TextView mUploadedBillCountTv;
    @BindView(R.id.tv_rn_stored_bill_count) TextView mStoredBillCountTv;

    private RNBucketAdapter mAdapter;

    private List<RNBucket> mBuckets = new ArrayList<>();

    private Map<String, RNBucket> mBucketsMap = new HashMap<>();

    private String mReadEPC;

    private String[] mLinks;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillStoredMessage message) {
        increaseCount(mStoredBillCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUploadedMessage message) {
        if (message.isFromDB) {
            decreaseCount(mStoredBillCountTv);
        }
        increaseCount(mUploadedBillCountTv);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
//        String content = message.isReadNone ?
//                "未发现桶标签" : "发现弱标签：" + message.weakBodyCode;
//        mHints.add(0, new Hint(content));
//        mHintAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            switch (CommonUtils.getEPCType(message.epc)) {
                case BUCKET:
                    String epcStr = CommonUtils.bytesToHex(message.epc);
                    if (!mBucketsMap.containsKey(epcStr)) {
                        RNBucket bucket = new RNBucket(message.epc);
                        mBuckets.add(0, bucket);
                        mBucketsMap.put(epcStr, bucket);
                        increaseCount(mBucketCountTv);
                        playSound();
                    } else {
                        if (mReadEPC != null) {
                            mBucketsMap.get(mReadEPC).setHighlight(false);
                        }
                        mBucketsMap.get(epcStr).setHighlight(true);
                    }
                    mReadEPC = epcStr;
                    mAdapter.notifyDataSetChanged();
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        sendAdminLoginMessage(CommonUtils.bytesToHex(message.epc));
                        ConfigActivity.actionStart(getContext());
                    }
                    break;
                default:
                    if (mBucketsMap.containsKey(mReadEPC)) {
                        mBucketsMap.get(mReadEPC).setHighlight(false);
                        mAdapter.notifyDataSetChanged();
                    }
                    mReadEPC = null;
            }
        } else {
            mAdminCardScannedCount = 0;
            if (mBucketsMap.containsKey(mReadEPC)) {
                mBucketsMap.get(mReadEPC).setHighlight(false);
                mAdapter.notifyDataSetChanged();
            }
            mReadEPC = null;
        }
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mStoredBillCountTv.setText(String.valueOf(MyVars.cache.getStoredDealerBill()));
        mLinks = getResources().getStringArray(R.array.rn_link);
        mRNLinkSpn.setText(mLinks[0]);
        mRNLinkSpn.setAdapter(new ArrayAdapter<>(mContext, R.layout.item_rn_link, mLinks));
        ArrayAdapter<String> mDriverAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_dropdown_item_1line,
                Objects.requireNonNull(ConfigHelper.getString(MyParams.S_DRIVER_HISTORY).split(",")));
        mRNDriverAct.setAdapter(mDriverAdapter);
        mRNDriverAct.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    ((AutoCompleteTextView) v).showDropDown();
            }
        });
        ArrayAdapter<String> mCounterAdapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_dropdown_item_1line,
                Objects.requireNonNull(ConfigHelper.getString(MyParams.S_COUNTER_HISTORY).split(",")));
        mRNCounterAct.setAdapter(mCounterAdapter);
        mRNCounterAct.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    ((AutoCompleteTextView) v).showDropDown();
            }
        });

        mAdapter = new RNBucketAdapter(mBuckets);
        mAdapter.enableSwipeItem();
        mAdapter.setOnItemSwipeListener(new OnItemSwipeListener() {

            @Override
            public void onItemSwipeStart(RecyclerView.ViewHolder viewHolder, int pos) {}

            @Override
            public void clearView(RecyclerView.ViewHolder viewHolder, int pos) {}

            @Override
            public void onItemSwipeMoving(Canvas canvas, RecyclerView.ViewHolder viewHolder, float dX, float dY, boolean isCurrentlyActive) {}

            @Override
            public void onItemSwiped(RecyclerView.ViewHolder viewHolder, int pos) {
                String epcStr = mBuckets.get(pos).getEpc();
                mBucketsMap.remove(epcStr);
                if (epcStr.equals(mReadEPC)) {
                    mReadEPC = null;
                }
                decreaseCount(mBucketCountTv);
            }
        });

        ItemDragAndSwipeCallback itemDragAndSwipeCallback = new ItemDragAndSwipeCallback(mAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(itemDragAndSwipeCallback);
        itemTouchHelper.attachToRecyclerView(mBucketsRv);

        mBucketsRv.setLayoutManager(new GridLayoutManager(getContext(), 3));
        mBucketsRv.setAdapter(mAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_rn;
    }

    @OnClick(R.id.btn_rn_clear)
    void onClearButtonClicked() {
        //DialogActivity.actionStart(getContext(), "");
        new MaterialDialog.Builder(mContext)
                .title("提示信息")
                .content("确认清空当前扫描桶列表吗？")
                .positiveText("确认")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消")
                .negativeColorRes(R.color.gray)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        clearBuckets();
                        dialog.dismiss();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    @OnClick(R.id.btn_rn_commit)
    void onCommitButtonClicked() {
        new MaterialDialog.Builder(mContext)
                .title("提示信息")
                .content("确认提交当前的" + mRNLinkSpn.getText().toString() + "吗？")
                .positiveText("确认")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消")
                .negativeColorRes(R.color.gray)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        String stage = "";
                        for (int i = 0; i < mLinks.length; i++) {
                            if (mLinks[i].equals(mRNLinkSpn.getText().toString())) {
                                stage = "1" + String.valueOf(i);
                                break;
                            }
                        }
                        String counter = mRNCounterAct.getText().toString();
                        String driver = mRNDriverAct.getText().toString();
                        saveHistory(mRNCounterAct, MyParams.S_COUNTER_HISTORY);
                        saveHistory(mRNDriverAct, MyParams.S_DRIVER_HISTORY);
                        final MessageDealer dealer = new MessageDealer(stage, counter, driver);
                        for (RNBucket bucket : mBuckets) {
                            dealer.addBucket(System.currentTimeMillis() / 1000, bucket.getEpc());
                        }
                        NetHelper.getInstance().uploadDealerMessage(dealer).enqueue(new Callback<Reply>() {
                            @Override
                            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                                Reply body = response.body();
                                Log.i(TAG, body.toString());
                                if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                                    MyVars.cache.storeDealerBill(dealer);
                                    EventBus.getDefault().post(new BillStoredMessage());
                                } else {
                                    EventBus.getDefault().post(new BillUploadedMessage(false));
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                                MyVars.cache.storeDealerBill(dealer);
                                EventBus.getDefault().post(new BillStoredMessage());
                            }
                        });
                        clearBuckets();
                        dialog.dismiss();
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void saveHistory(AutoCompleteTextView input, String keyword) {
        String history = ConfigHelper.getString(keyword);
        String newWord = input.getText().toString();
        if (!TextUtils.isEmpty(newWord) && !history.contains(newWord + ",")) {
            ConfigHelper.setParam(keyword, history + newWord + ",");
            ((ArrayAdapter<String>) input.getAdapter()).add(newWord);
            ((ArrayAdapter<String>) input.getAdapter()).remove("");
        }
        input.setText("");
    }

    private void clearBuckets() {
        mBuckets.clear();
        mBucketsMap.clear();
        mBucketCountTv.setText("0");
        mReadEPC = null;
    }
}
