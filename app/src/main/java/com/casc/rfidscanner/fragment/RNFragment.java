package com.casc.rfidscanner.fragment;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.RNBucketAdapter;
import com.casc.rfidscanner.backend.InstructionHandler;
import com.casc.rfidscanner.bean.RNBucket;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageDealer;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.callback.ItemDragAndSwipeCallback;
import com.chad.library.adapter.base.listener.OnItemSwipeListener;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 水站库存管理Fragment
 */

public class RNFragment extends BaseFragment implements InstructionHandler {

    private static final String TAG = RNFragment.class.getSimpleName();
    private static final int BUCKET_FOUND_READ_COUNT = 5;
    // Constant for InnerHandler message.what
    private static final int MSG_INCREASE_SCANNED_COUNT = 0;
    private static final int MSG_UPDATE_BUCKET_LIST = 1;

    @BindView(R.id.spn_rn_link) BetterSpinner mRNLinkSpn;
    @BindView(R.id.rv_rn_buckets) RecyclerView mBucketsRv;
    @BindView(R.id.tv_rn_bucket_count) TextView mBucketCountTv;

    @BindView(R.id.tv_rn_uploaded_bill_count) TextView mUploadedBillCountTv;
    @BindView(R.id.tv_rn_stored_bill_count) TextView mStoredBillCountTv;

    private RNBucketAdapter mAdapter;

    private List<RNBucket> mBuckets = new ArrayList<>();

    private Map<String, RNBucket> mBucketsMap = new HashMap<>();

    private String mReadEPC;

    private int mReadCount;

    private String[] mLinks;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

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

    @Override
    protected void initFragment() {
        mStoredBillCountTv.setText(String.valueOf(MyVars.cache.getStoredDealerBill()));
        mLinks = getResources().getStringArray(R.array.rn_link);
        mRNLinkSpn.setText(mLinks[0]);
        mRNLinkSpn.setAdapter(new ArrayAdapter<>(getContext(), R.layout.item_rn_link, mLinks));

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
                    mReadCount = 0;
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

    @Override
    public void deal(byte[] ins) {
        int command = ins[2] & 0xFF;
        switch (command) {
            case 0x22: // 轮询成功的处理流程
                int pl = ((ins[3] & 0xFF) << 8) + (ins[4] & 0xFF);
                byte[] epc = Arrays.copyOfRange(ins, 8, pl + 3);
                MyParams.EPCType epcType = CommonUtils.getEPCType(epc);
                switch (epcType) {
                    case BUCKET:
                        String epcStr = CommonUtils.bytesToHex(epc);
                        if (epcStr.equals(mReadEPC)) {
                            mReadCount++;
                            if (mReadCount == BUCKET_FOUND_READ_COUNT) {
                                playSound();
                                if (!mBucketsMap.containsKey(epcStr)) {
                                    RNBucket bucket = new RNBucket(epc);
                                    mBuckets.add(0, bucket);
                                    mBucketsMap.put(epcStr, bucket);
                                    mHandler.sendMessage(Message.obtain(mHandler, MSG_INCREASE_SCANNED_COUNT));
                                } else {
                                    mBucketsMap.get(epcStr).setHighlight(true);
                                }
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_BUCKET_LIST));
                            }
                        } else {
                            if (mReadEPC != null && mBucketsMap.get(mReadEPC) != null) {
                                mBucketsMap.get(mReadEPC).setHighlight(false);
                                mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_BUCKET_LIST));
                            }
                            mReadEPC = epcStr;
                            mReadCount = 1;
                        }
                        break;
                    case CARD_ADMIN:
                        mAdminCardScannedCount++;
                        if (mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                            sendAdminLoginMessage(CommonUtils.bytesToHex(epc));
                            ConfigActivity.actionStart(getContext());
                        }
                        break;
                    default:
                        if (mReadEPC != null && mBucketsMap.get(mReadEPC) != null) {
                            mBucketsMap.get(mReadEPC).setHighlight(false);
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_BUCKET_LIST));
                        }
                        mReadEPC = null;
                        mReadCount = 0;
                }
                break;
            default: // 命令帧执行失败的处理流程
                switch (ins[5] & 0xFF) {
                    case 0x15: // 轮询失败
                        mAdminCardScannedCount = 0;
                        if (mReadEPC != null && mBucketsMap.get(mReadEPC) != null) {
                            mBucketsMap.get(mReadEPC).setHighlight(false);
                            mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_BUCKET_LIST));
                        }
                        mReadEPC = null;
                        mReadCount = 0;
                        break;
                }
        }
    }

    @OnClick(R.id.btn_rn_clear)
    void onClearButtonClicked() {
        //DialogActivity.actionStart(getContext(), "");
        new MaterialDialog.Builder(getContext())
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
        new MaterialDialog.Builder(getContext())
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
                        final MessageDealer dealer = new MessageDealer(stage);
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

    private void clearBuckets() {
        mBuckets.clear();
        mBucketsMap.clear();
        mBucketCountTv.setText("0");
        mReadEPC = null;
        mReadCount = 0;
    }

    private static class InnerHandler extends Handler {

        private WeakReference<RNFragment> mOuter;

        InnerHandler(RNFragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            RNFragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_INCREASE_SCANNED_COUNT:
                    outer.increaseCount(outer.mBucketCountTv);
                    break;
                case MSG_UPDATE_BUCKET_LIST:
                    outer.mAdapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}
