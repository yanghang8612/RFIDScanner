package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.ConfigActivity;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MsgOnline;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.AbnormalBucketMessage;
import com.casc.rfidscanner.message.OnlineUploadedMessage;
import com.casc.rfidscanner.message.PollingResultMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.casc.rfidscanner.view.NumberSwitcher;
import com.google.gson.Gson;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final int MSG_UPDATE_TASK_LIST = 0;
    private static final int MSG_START_TASK = 1;
    private static final int MSG_STOP_TASK = 2;
    private static final int MSG_TAG_SCANNED = 3;
    private static final int MSG_TAG_STORED = 4;

    @BindView(R.id.ns_r7_scanned_count) NumberSwitcher mScannedCountNs;
    @BindView(R.id.ns_r7_uploaded_count) NumberSwitcher mUploadedCountNs;
    @BindView(R.id.ns_r7_stored_count) NumberSwitcher mStoredCountNs;
    @BindView(R.id.ll_task_choose) LinearLayout mTaskChooseLl;
    @BindView(R.id.spn_task_list) BetterSpinner mTaskListSpn;
    @BindView(R.id.ll_task_info) LinearLayout mTaskInfoLl;
    @BindView(R.id.tv_task_id) TextView mTaskIDTv;
    @BindView(R.id.tv_task_product_name) TextView mProductNameTv;
    @BindView(R.id.tv_task_product_count) TextView mProductCountTv;

    // 提示信息列表
    private List<String> mTasks = new ArrayList<>();

    private ArrayAdapter<String> mAdapter;

    private String mSelectedTaskID;

    private Set<String> mBuckets = new HashSet<>();

    private Set<String> mErrors = new HashSet<>();

    private boolean mTaskStarted;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(OnlineUploadedMessage message) {
        if (message.isFromDB) {
            mUploadedCountNs.increaseNumber();
            mStoredCountNs.decreaseNumber();
        } else {
            mUploadedCountNs.increaseNumber();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(AbnormalBucketMessage message) {
//        String content = message.isReadNone ?
//                "未发现桶标签" : "发现弱标签：" + new Bucket(message.epc).getBodyCode();
//        mHints.add(0, new Hint(content));
//        mHintAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(PollingResultMessage message) {
        if (message.isRead) {
            String epcStr = CommonUtils.bytesToHex(message.epc);
            switch (CommonUtils.validEPC(message.epc)) {
                case NONE: // 检测到未注册标签，是否提示
                    break;
                case BUCKET:
                    if (mTaskStarted && !mBuckets.contains(epcStr)) {
                        playSound();
                        mBuckets.add(epcStr);
                        Message.obtain(mHandler, MSG_TAG_SCANNED).sendToTarget();
                        final MsgOnline online = new MsgOnline().addBucket(mSelectedTaskID, epcStr);
                        NetHelper.getInstance().uploadOnlineMsg(online).enqueue(new Callback<Reply>() {
                            @Override
                            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                                Reply body = response.body();
                                if (response.isSuccessful() && body != null && body.getCode() == 200) {
                                    EventBus.getDefault().post(new OnlineUploadedMessage(false));
                                } else {
                                    MyVars.cache.storeOnlineMessage(online);
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                                MyVars.cache.storeOnlineMessage(online);
                            }
                        });
                    }
                    if (!mTaskStarted && !mErrors.contains(epcStr)) {
                        mErrors.add(epcStr);
                        NetHelper.getInstance().sendLogRecord("非法上线: " + Bucket.getBodyCode(epcStr));
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
        mScannedCountNs.setNumber(0);
        mUploadedCountNs.setNumber(0);
        mStoredCountNs.setNumber((int) MyVars.cache.getStoredCount());

        mAdapter = new ArrayAdapter<>(mContext, android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>());
        mTaskListSpn.setAdapter(mAdapter);
        mTaskListSpn.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!TextUtils.isEmpty(s)) {
                    String[] result = s.toString().split(" ");
                    mSelectedTaskID = result[0];
                    mTaskIDTv.setText(result[0]);
                    mProductNameTv.setText(result[1]);
                    mProductCountTv.setText(result[2]);
                }
            }
        });
        MyVars.fragmentExecutor.scheduleWithFixedDelay(new QueryProductionTasks(), 1, 10, TimeUnit.SECONDS);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r7;
    }

    @OnClick(R.id.btn_task_start)
    void onTaskStartBtnClicked() {
        if (mSelectedTaskID == null) {
            if (mTasks.isEmpty()) {
                showToast("目前暂无生产任务单,请联系生产主管");
            } else {
                showToast("请选择要启动的任务单");
            }
        } else {
            showDialog("确认开始任务单(" + mTaskListSpn.getText().toString() + ")吗？", new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    NetHelper.getInstance().startProductionTask(mSelectedTaskID).enqueue(new Callback<Reply>() {
                        @Override
                        public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                            Reply reply = response.body();
                            if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                                Message.obtain(mHandler, MSG_START_TASK).sendToTarget();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {

                        }
                    });
                }
            });
        }
    }

    @OnClick(R.id.btn_task_stop)
    void onTaskStopBtnClicked() {
        showDialog("确认提交任务单" + mTaskListSpn.getText().toString() + "吗？", new MaterialDialog.SingleButtonCallback() {
            @Override
            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                NetHelper.getInstance().stopProductionTask(mSelectedTaskID).enqueue(new Callback<Reply>() {
                    @Override
                    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                        Reply reply = response.body();
                        if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                            Message.obtain(mHandler, MSG_STOP_TASK).sendToTarget();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {

                    }
                });
            }
        });
    }

    private void showDialog(String content, MaterialDialog.SingleButtonCallback callback) {
        new MaterialDialog.Builder(mContext)
                .content(content)
                .positiveText("确认")
                .positiveColorRes(R.color.white)
                .btnSelector(R.drawable.md_btn_postive, DialogAction.POSITIVE)
                .negativeText("取消")
                .negativeColorRes(R.color.gray)
                .onPositive(callback)
                .show();
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
                case MSG_UPDATE_TASK_LIST:
                    String curTask = outer.mTaskListSpn.getText().toString();
                    if (!TextUtils.isEmpty(curTask) && !outer.mTasks.contains(curTask)) {
                        outer.mTaskListSpn.setText("");
                    }
                    outer.mAdapter.clear();
                    outer.mAdapter.addAll(outer.mTasks);
                    outer.mAdapter.getFilter().filter("");
                    outer.mAdapter.notifyDataSetChanged();
                    break;
                case MSG_START_TASK:
                    outer.mScannedCountNs.setNumber(0);
                    outer.mUploadedCountNs.setNumber(0);
                    outer.mTaskChooseLl.setVisibility(View.GONE);
                    outer.mTaskInfoLl.setVisibility(View.VISIBLE);
                    outer.mTaskStarted = true;
                    break;
                case MSG_STOP_TASK:
                    outer.mTaskChooseLl.setVisibility(View.VISIBLE);
                    outer.mTaskInfoLl.setVisibility(View.GONE);
                    outer.mTaskListSpn.setText("");
                    outer.mSelectedTaskID = null;
                    outer.mTaskStarted = false;
                    break;
                case MSG_TAG_SCANNED:
                    outer.mScannedCountNs.increaseNumber();
                    break;
                case MSG_TAG_STORED:
                    outer.mStoredCountNs.increaseNumber();
                    break;
            }
        }
    }

    private class QueryProductionTasks implements Runnable {

        private class Task {

            private String taskid;

            private String productname;

            private int number;

            private char flag;

            @Override
            public String toString() {
                return taskid + " " + productname + " " + number + "桶";
            }
        }

        private class Tasks {

            private List<Task> taskinfo;

            public List<Task> getTasks() {
                return taskinfo;
            }
        }

        @Override
        public void run() {
            try {
                Response<Reply> response = NetHelper.getInstance().queryProductionTask().execute();
                Reply reply = response.body();
                if (response.isSuccessful() && reply != null && reply.getCode() == 200) {
                    mTasks.clear();
                    List<Task> tasks = new Gson().fromJson(reply.getContent(), Tasks.class).getTasks();
                    if (tasks != null) {
                        for (final Task task : tasks) {
                            if (task.flag == '0') {
                                mTasks.add(task.toString());
                            } else if (!mTaskStarted) {
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSelectedTaskID = task.taskid;
                                        mTaskIDTv.setText(task.taskid);
                                        mProductNameTv.setText(task.productname);
                                        mProductCountTv.setText(task.number + "(桶)");
                                        Message.obtain(mHandler, MSG_START_TASK).sendToTarget();
                                    }
                                });
                            }
                        }
                    }
                    Message.obtain(mHandler, MSG_UPDATE_TASK_LIST).sendToTarget();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
