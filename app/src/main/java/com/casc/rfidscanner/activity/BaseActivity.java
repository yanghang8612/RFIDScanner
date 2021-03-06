package com.casc.rfidscanner.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.casc.rfidscanner.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import butterknife.ButterKnife;

public abstract class BaseActivity extends AppCompatActivity implements View.OnSystemUiVisibilityChangeListener {

    private Toast mToast;
    private View mDecorView;
    private int mVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE;

    // 派生类必须重写该abstract方法，以实现自己的Activity初始化逻辑
    protected abstract void initActivity();

    // 派生类所加载的LayoutID
    protected abstract int getLayout();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayout());
        setTitle("");

        ActivityCollector.addActivity(this);
        ButterKnife.bind(this);
        initActivity();

        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mDecorView = getWindow().getDecorView();
        mDecorView.setSystemUiVisibility(mVisibility);
        mDecorView.setOnSystemUiVisibilityChangeListener(this);
    }

    @Override
    public void finish() {
        super.finish();
        if (!(ActivityCollector.getTopActivity() instanceof ConfigActivity)) {
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
        mToast.cancel();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(!hasFocus)
        {
            @SuppressLint("WrongConstant")
            Object service = getApplicationContext().getSystemService("statusbar");
            try {
                @SuppressLint("PrivateApi")
                Class<?> statusBarManager = Class.forName("android.app.StatusBarManager");
                Method collapse = statusBarManager.getMethod("collapsePanels");
                collapse.setAccessible(true);
                collapse.invoke(service);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSystemUiVisibilityChange(int visibility) {
        mDecorView.setSystemUiVisibility(mVisibility);
    }

    /**
     * 判定某个坐标为x、y的点是否在view范围内
     */
    protected boolean isTouchPointInView(View view, int x, int y) {
        if (view == null) {
            return false;
        }
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int left = location[0];
        int top = location[1];
        int right = left + view.getMeasuredWidth();
        int bottom = top + view.getMeasuredHeight();
        //view.isClickable() &&
        return y >= top && y <= bottom && x >= left
                && x <= right;
    }

    /**
     * 通过重写Activity的该方法，监视用户点击事件，判断是否需要隐藏软键盘
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (isShouldHideKeyboard(v, ev)) {
                hideKeyboard();
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 根据 EditText 所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘
     * @param v 判定所依据的View实例
     * @param event 从用户点击的MotionEvent中获取用户点击的坐标
     * @return 点击在EditText或键盘外时，返回true，否则返回false
     */
    private boolean isShouldHideKeyboard(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left + v.getWidth();
            return !(event.getX() > left && event.getX() < right
                    && event.getY() > top && event.getY() < bottom);
        }
        return false;
    }

    /**
     * 获取当前Activity名为android.R.id.content的View实例
     * @return 当前Activity的内容View实例
     */
    public View getContentView(){
        ViewGroup views = (ViewGroup) this.getWindow().getDecorView();
        FrameLayout content = views.findViewById(android.R.id.content);
        return content.getChildAt(0);
    }

    /**
     * 通过调用系统服务，隐藏当前显示的软键盘
     */
    protected void hideKeyboard() {
        getContentView().clearFocus();
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    /**
     * 在当前Activity的上下文中打印一条短消息
     * @param content 消息内容
     */
    public void showToast(String content) {
        mToast.setText(content);
        mToast.show();
    }

    /**
     * 在当前Activity的上下文中打印一条短消息
     * @param resId 消息的资源ID
     */
    public void showToast(@StringRes int resId) {
        mToast.setText(resId);
        mToast.show();
    }

    protected void showDialog(String content, MaterialDialog.SingleButtonCallback callback) {
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
