package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.SafeCodeActivity;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.MultiStatusMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 各阶段Fragment的基类
 */
public abstract class BaseFragment extends Fragment {

    private static final String TAG = BaseFragment.class.getSimpleName();

    // Fragment所属Activity上下文
    protected Context mContext;

    // 运维专用卡扫描计数器以及声音资源ID
    protected int mAdminCardScannedCount, mSoundID;

    // 播放实例
    private SoundPool mSoundPool =
            new SoundPool.Builder()
                    .setMaxStreams(10)
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .build();

    // 提示框实例
    private Toast mToast;

    // 系统震动辅助类
    protected Vibrator mVibrator;

    @BindView(R.id.ll_connection_status) LinearLayout mConnectionStatusLl;
    @BindView(R.id.iv_reader_status) ImageView mReaderStatusIv;
    @BindView(R.id.iv_network_status ) ImageView mNetworkStatusIv;
    @BindView(R.id.iv_platform_status) ImageView mPlatformStatusIv;
    @BindView(R.id.tv_time_hour) TextView mTimeHourIv;
    @BindView(R.id.tv_time_colon) TextView mTimeColonIv;
    @BindView(R.id.tv_time_minute) TextView mTimeMinuteIv;

    @OnLongClick(R.id.btn_backdoor) boolean onBackDoorButtonClicked() {
        if (MyParams.ENABLE_BACKDOOR) {
            SafeCodeActivity.actionStart(mContext);
            return true;
        }
        return false;
    }

    @CallSuper
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MultiStatusMessage message) {
        mReaderStatusIv.setImageResource(message.readerStatus ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mNetworkStatusIv.setImageResource(message.networkStatus ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mPlatformStatusIv.setImageResource(message.platformStatus ?
                R.drawable.ic_connection_normal : R.drawable.ic_connection_abnormal);
        mTimeHourIv.setText(
                new SimpleDateFormat("HH", Locale.CHINA).format(new Date(System.currentTimeMillis())));
        mTimeMinuteIv.setText(
                new SimpleDateFormat("mm", Locale.CHINA).format(new Date(System.currentTimeMillis())));
        mTimeColonIv.setVisibility(
                mTimeColonIv.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        mContext = getActivity();
        mSoundID = mSoundPool.load(mContext, R.raw.timer, 1);
        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
        LinearLayout layout = (LinearLayout) mToast.getView();
        TextView tv = (TextView) layout.getChildAt(0);
        tv.setTextSize(24);
        mToast.setGravity(Gravity.CENTER, 0, 0);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(getLayout(), container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initFragment();
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdminCardScannedCount = 0;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mToast.cancel();
        EventBus.getDefault().unregister(this);
        MyVars.cache.clear();
    }

    // 派生类必须重写该abstract方法，以实现自己的Fragment初始化逻辑
    protected abstract void initFragment();

    // 派生类所加载的LayoutID
    protected abstract int getLayout();

    protected void playSound() {
        mSoundPool.play(mSoundID, 1, 1, 10, 0, 1.0F);
    }

    protected void increaseCount(TextView view) {
        int count = Integer.valueOf(view.getText().toString());
        view.setText(String.valueOf(++count));
    }

    protected void decreaseCount(TextView view) {
        int count = Integer.valueOf(view.getText().toString());
        view.setText(String.valueOf(--count));
    }

    protected void showToast(String content) {
        mToast.setText(content);
        mToast.show();
    }

    protected void showToast(@StringRes int contentRes) {
        mToast.setText(contentRes);
        mToast.show();
    }

    protected void showDialog(String content, MaterialDialog.SingleButtonCallback callback) {
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
}
