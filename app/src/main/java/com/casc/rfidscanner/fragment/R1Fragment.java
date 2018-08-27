package com.casc.rfidscanner.fragment;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.adapter.BucketAdapter;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageScrap;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.MultiStatusMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Arrays;
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
 * 桶报废Fragment
 */
public class R1Fragment extends BaseFragment {

    private static final String TAG = R1Fragment.class.getSimpleName();
    private static final int CAN_SCRAP_READ_COUNT = 5;
    private static final int MAX_READ_NONE_COUNT = 3;
    // Constant for InnerHandler message.what

    @BindView(R.id.iv_scrap_tag_status) ImageView mTagStatusIv;
    @BindView(R.id.tv_scrap_body_code) TextView mBodyCodeTv;
    @BindView(R.id.tv_scrap_product_name) TextView mProductNameTv;
    @BindView(R.id.spn_scrap_reason) BetterSpinner mScrapReasonSpn;
    @BindView(R.id.btn_confirm_scrap) Button mConfirmScrapBtn;
    @BindView(R.id.rv_scrap_products) RecyclerView mScrapProductsRv;

    // 已报废桶列表
    private List<Bucket> mBuckets = new ArrayList<>();

    // 已报废桶Map
    private Map<String, Bucket> mBucketsMap = new HashMap<>();

    // 已报废桶列表适配器
    private BucketAdapter mAdapter;

    // 当前读取的EPC
    private byte[] mScannedEPC;

    // 读取到和未读取到EPC的计数器
    private int mReadCount, mReadNoneCount;

    // 报废所必需的相关元素标志位
    private boolean mIsBucketEPCRead, mIsAllConnectionsReady;

    // 要报废的桶实例
    private Bucket mBucketToScrap;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        super.onMessageEvent(message);
        if (message.readerStatus && message.networkStatus && message.platformStatus) {
            mIsAllConnectionsReady = true;
            mConfirmScrapBtn.setEnabled(canScrap());
        } else {
            mIsAllConnectionsReady = false;
            mConfirmScrapBtn.setEnabled(false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        updateConfigViews();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            MyParams.EPCType epcType = CommonUtils.validEPC(message.epc);
            switch (epcType) {
                case BUCKET:
                    mReadNoneCount = 0;
                    if (Arrays.equals(message.epc, mScannedEPC)) {
                        mReadCount += 1;
                    } else {
                        mReadCount = 0;
                        mScannedEPC = message.epc;
                        String epcStr = CommonUtils.bytesToHex(mScannedEPC);
                        if (mBucketsMap.containsKey(epcStr)) {
                            mBucketToScrap = mBucketsMap.get(epcStr);
                        } else {
                            mBucketToScrap = new Bucket(mScannedEPC);
                            mBucketsMap.put(epcStr, mBucketToScrap);
                        }
                    }
                    if (mReadCount > CAN_SCRAP_READ_COUNT) {
                        mIsBucketEPCRead = true;
                        mTagStatusIv.setImageResource(R.drawable.ic_connection_normal);
                        mBodyCodeTv.setText(mBucketToScrap.getBodyCode()
                                + (mBucketToScrap.isScraped() ? "(已报废)" : ""));
                        mProductNameTv.setText(mBucketToScrap.getName());
                        mConfirmScrapBtn.setEnabled(canScrap());
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        sendAdminLoginMessage(CommonUtils.bytesToHex(mScannedEPC));
                        ConfigActivity.actionStart(getContext());
                    }
                    break;
            }
        } else {
            mAdminCardScannedCount = 0;
            if (++mReadNoneCount > MAX_READ_NONE_COUNT) {
                mReadCount = 0;
                mScannedEPC = null;
                mIsBucketEPCRead = false;
                mConfirmScrapBtn.setEnabled(false);
                mTagStatusIv.setImageResource(R.drawable.ic_connection_abnormal);
                mBodyCodeTv.setText("");
                mProductNameTv.setText("");
            }
        }
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        updateConfigViews();
        mAdapter = new BucketAdapter(mBuckets);
        mScrapProductsRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mScrapProductsRv.setAdapter(mAdapter);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r1;
    }

    @OnClick(R.id.btn_confirm_scrap)
    public void onButtonClicked(View view) {
        new MaterialDialog.Builder(Objects.requireNonNull(getContext()))
                .title("提示信息")
                .content("桶身码：" + mBodyCodeTv.getText() + "\n" + "报废原因：" + mScrapReasonSpn.getText())
                .positiveText("确认报废")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消报废")
                .negativeColorRes(R.color.gray)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                        // TODO: 2018/8/9 disablecode可能为空，bug
                        MessageScrap message = new MessageScrap();
                        message.addBucket("", mBucketToScrap.getEpcStr(),
                                MyVars.config.getDisableInfoByWord(mScrapReasonSpn.getText().toString()).getCode());
                        NetHelper.getInstance().uploadR1Message(message).enqueue(new Callback<Reply>() {
                            @Override
                            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                                if (response.isSuccessful()) {
                                    mBucketToScrap.setScraped();
                                    mBuckets.add(mBucketToScrap);
                                    mAdapter.notifyDataSetChanged();
                                }
                                dialog.dismiss();
                            }

                            @Override
                            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {

                            }
                        });

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

    private boolean canScrap() {
        return MyVars.getReader().isConnected() && mIsBucketEPCRead && mIsAllConnectionsReady
                && mBucketToScrap != null && !mBucketToScrap.isScraped();
    }

    private void updateConfigViews() {
        mScrapReasonSpn.setAdapter(new ArrayAdapter<>(mContext, R.layout.item_common,
                MyVars.config.getDisableInfo()));

        String curScrapReason = mScrapReasonSpn.getText().toString();
        if (TextUtils.isEmpty(curScrapReason)
                || MyVars.config.getDisableInfoByWord(curScrapReason) == null) {
            if (!MyVars.config.getDisableInfo().isEmpty()) {
                mScrapReasonSpn.setText(MyVars.config.getDisableInfo().get(0).getWord());
            } else {
                mScrapReasonSpn.setText("");
            }
        }
    }
}
