package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.message.ConfigUpdatedMessage;
import com.casc.rfidscanner.message.TaskConfiguredMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.view.InputCodeLayout;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TaskConfigureActivity extends BaseActivity {

    private static final String TAG = TaskConfigureActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        if (!(ActivityCollector.getTopActivity() instanceof TaskConfigureActivity)) {
            Intent intent = new Intent(context, TaskConfigureActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(intent);
        }
    }

    @BindView(R.id.btn_start_task) Button mStartTaskBtn;
    @BindView(R.id.spn_task_product_name) BetterSpinner mTaskProductNameSpn;
    @BindView(R.id.icl_task_product_count) InputCodeLayout mTaskProductCountIcl;
    @BindView(R.id.spn_task_amount) BetterSpinner mTaskAmountSpn;

    // 系统震动辅助类
    private Vibrator mVibrator;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(ConfigUpdatedMessage message) {
        updateConfigViews();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_configure);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        updateConfigViews();

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        mTaskAmountSpn.setAdapter(new ArrayAdapter<>(this,
                R.layout.item_common, getResources().getStringArray(R.array.stack_capacity)));
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @OnClick(R.id.btn_cancel_task)
    void onCancelTaskButtonClicked() {
        finish();
    }

    @OnClick(R.id.btn_start_task)
    void onStartTaskButtonClicked() {
        String productName = mTaskProductNameSpn.getText().toString();
        String productCount = mTaskProductCountIcl.getCode();
        String taskAmount = mTaskAmountSpn.getText().toString();
        if (TextUtils.isEmpty(productName)) {
            showToast("请选择待上线产品");
        } else if (TextUtils.isEmpty(productCount)) {
            showToast("请输入待上线数量");
        } else if (TextUtils.isEmpty(taskAmount)) {
            showToast("请选择每一垛数量");
        } else {
            EventBus.getDefault().post(new TaskConfiguredMessage(productName,
                    Integer.valueOf(productCount),
                    Integer.valueOf(taskAmount.substring(0, 2))));
            finish();
        }
    }

    @OnClick({
            R.id.cv_keyboard_one, R.id.cv_keyboard_two, R.id.cv_keyboard_three,
            R.id.cv_keyboard_four, R.id.cv_keyboard_five, R.id.cv_keyboard_six,
            R.id.cv_keyboard_seven, R.id.cv_keyboard_eight, R.id.cv_keyboard_nine,
            R.id.cv_keyboard_zero})
    void onKeyboardClicked(CardView view) {
        mVibrator.vibrate(30);
        TextView textView = (TextView) view.getChildAt(0);
        mTaskProductCountIcl.addCode(textView.getText().toString());
    }

    @OnClick(R.id.cv_keyboard_clear)
    void onKeyboardClearClicked() {
        mVibrator.vibrate(80);
        mTaskProductCountIcl.clear();
    }

    @OnClick(R.id.cv_keyboard_back)
    void onKeyboardBackClicked() {
        mVibrator.vibrate(50);
        mTaskProductCountIcl.deleteCode();
    }

    private void updateConfigViews() {
        mTaskProductNameSpn.setAdapter(new ArrayAdapter<>(MyApplication.getInstance(),
                R.layout.item_common, MyVars.config.getProductInfo()));
    }
}
