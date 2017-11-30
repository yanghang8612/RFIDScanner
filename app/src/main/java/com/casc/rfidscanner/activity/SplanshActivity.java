package com.casc.rfidscanner.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;

import com.casc.rfidscanner.R;

import org.xutils.view.annotation.ContentView;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
@ContentView(R.layout.activity_splansh)
public class SplanshActivity extends BaseActivity {
    private final int SPLASH_DISPLAY_LENGHT = 3000;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        //获取sp登录信息
        Boolean isLogin=false;
        SharedPreferences sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE); //私有数据
        String  name = sharedPreferences.getString("name","");
        String pwd = sharedPreferences.getString("pwd","");
        if (name!=""&&isLeagal(name,pwd))
            isLogin = true;

        mHandler = new Handler();
        // 延迟SPLASH_DISPLAY_LENGHT时间然后跳转到MainActivity
        mHandler.postDelayed(new DelayRunnable(this,isLogin), 2000);

    }
    static class DelayRunnable implements Runnable {
        Context context;
        Boolean isLogin;
        public DelayRunnable(Context context,Boolean isLogin){
            this.context = context;
            this.isLogin = isLogin;

        }
        @Override
        public void run() {
            if (isLogin)
                context.startActivity(new Intent(context, MainActivity.class));
            else
                context.startActivity(new Intent(context,LoginActivity.class));
            ((Activity) context).finish();
        }
    }

    public Boolean isLeagal(String name , String pwd)
    {
        return true;
    }
}
