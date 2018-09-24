package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.activity.TaskConfigureActivity;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageOnline;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.message.TagCountChangedMessage;
import com.casc.rfidscanner.message.TaskConfiguredMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 桶上线Fragment
 */
public class R7Fragment extends BaseFragment {

    private static final String TAG = R7Fragment.class.getSimpleName();

    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_BACKGROUND = 0;

    @BindView(R.id.ll_r7_content) LinearLayout mR7ContentLl;
    @BindView(R.id.tv_task_product) TextView mTaskProductTv;
    @BindView(R.id.ll_task_configure) LinearLayout mTaskConfigureLl;
    @BindView(R.id.tv_task_configure_hint) TextView mTaskConfigureHintTv;
    @BindView(R.id.ll_online_bucket_count) LinearLayout mOnlineBucketCountLl;
    @BindView(R.id.ns_online_bucket_count) NumberSwitcher mOnlineBucketCountNs;
    @BindView(R.id.ns_stored_bill_count) NumberSwitcher mStoredBillCountNs;

    // 任务执行标志位及任务错误标志位
    private boolean mIsTaskExecuting, mIsTaskWrong;

    // 任务详细信息
    private MessageOnline mTask;

    // 执行任务中扫描到的桶EPC
    private Set<String> mBuckets = new HashSet<>();

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TagCountChangedMessage message) {
//        mScannedCountNs.setNumber(message.scannedCount);
//        mUploadedCountNs.setNumber(message.uploadedCount);
//        mStoredCountNs.setNumber(message.storedCount);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(TaskConfiguredMessage message) {
        showToast("任务已启动");
        mIsTaskExecuting = true;
        mTask = new MessageOnline(message.productName, message.productCount, message.amount);
        mTaskProductTv.setText(message.productName + "\n" + message.productCount + "桶");
        mTaskConfigureLl.getBackground().setTint(mContext.getColor(R.color.powder_blue));
        mTaskConfigureHintTv.setText("提交任务");
        mOnlineBucketCountNs.setNumber(0);
        mOnlineBucketCountLl.getBackground().setTint(mContext.getColor(R.color.powder_blue));
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillUploadedMessage message) {
        if (message.isFromDB) {
            mStoredBillCountNs.decreaseNumber();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillStoredMessage message) {
        mStoredBillCountNs.increaseNumber();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    if (!mBuckets.contains(epcStr)) {
                        mBuckets.add(epcStr);
                        mOnlineBucketCountNs.increaseNumber();
                        if (mIsTaskExecuting) {
                            mTask.addBucket(epcStr);
                            if (!mIsTaskWrong &&
                                    !Bucket.getProductInfo(epcStr).getName().equals(mTask.getProductname())) {
                                playAlert();
                                mIsTaskWrong = true;
                            } else {
                                playSound();
                            }
                        } else {
                            playSound();
                            mOnlineBucketCountLl.getBackground().setTint(mContext.getColor(R.color.red));
                            uploadOnlineMessage(new MessageOnline(epcStr));
                        }
                    }
                    break;
                case CARD_ADMIN:
                    if (++mAdminCardScannedCount == MyParams.ADMIN_CARD_SCANNED_COUNT) {
                        sendAdminLoginMessage(CommonUtils.bytesToHex(message.epc));
                        ConfigActivity.actionStart(mContext);
                    }
                    break;
            }
        } else {
            mAdminCardScannedCount = 0;
        }
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.VISIBLE);

        mOnlineBucketCountNs.setNumber(0);
        mStoredBillCountNs.setNumber(MyVars.cache.getStoredOnlineTaskCount());
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new FlickerTask(), 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r7;
    }

    @OnClick(R.id.ll_task_configure)
    void onTaskConfigureButtonClicked() {
        if (!mIsTaskExecuting) {
            TaskConfigureActivity.actionStart(mContext);
        } else {
            new MaterialDialog.Builder(mContext)
                    .title("提示信息")
                    .content("确认提交当前任务吗？")
                    .positiveText("确认")
                    .positiveColorRes(R.color.white)
                    .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                    .negativeText("取消")
                    .negativeColorRes(R.color.gray)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull final MaterialDialog dialog, @NonNull DialogAction which) {
                            uploadOnlineMessage(mTask.setEndTime(System.currentTimeMillis()));
                            mIsTaskExecuting = mIsTaskWrong = false;
                            mR7ContentLl.setBackgroundColor(mContext.getColor(R.color.white));
                            mTaskProductTv.setText("");
                            mTaskConfigureLl.getBackground().setTint(mContext.getColor(R.color.light_gray));
                            mTaskConfigureHintTv.setText("启动任务");
                            mOnlineBucketCountNs.setNumber(0);
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
    }

    private void uploadOnlineMessage(final MessageOnline online) {
        CommonUtils.generateRequestBody(online);
        if (MyVars.cache.getStoredOnlineTaskCount() == 0) {
            NetHelper.getInstance().uploadOnlineMessage(online).enqueue(new Callback<Reply>() {
                @Override
                public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                    Reply body = response.body();
                    if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                        MyVars.cache.storeOnlineTask(online);
                        EventBus.getDefault().post(new BillStoredMessage());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                    MyVars.cache.storeOnlineTask(online);
                    EventBus.getDefault().post(new BillStoredMessage());
                }
            });
        } else {
            MyVars.cache.storeOnlineTask(online);
            EventBus.getDefault().post(new BillStoredMessage());
        }
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R7Fragment> mOuter;

        InnerHandler(R7Fragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R7Fragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_UPDATE_BACKGROUND:
                    long sec = System.currentTimeMillis() / 1000;
                    outer.mR7ContentLl.setBackgroundColor(outer.mIsTaskWrong && sec % 2 == 0 ?
                            outer.mContext.getColor(R.color.red) : outer.mContext.getColor(R.color.white));
                    break;
            }
        }
    }

    private class FlickerTask implements Runnable {

        @Override
        public void run() {
            Message.obtain(mHandler, MSG_UPDATE_BACKGROUND).sendToTarget();
        }
    }
}
