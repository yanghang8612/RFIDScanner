package com.casc.rfidscanner.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.casc.rfidscanner.utils.ActivityCollector;

import org.xutils.x;

public abstract class BaseActivity extends AppCompatActivity {

    protected static final String DEFAULT_PRE = "default";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCollector.addActivity(this);
        x.view().inject(this);
        setTitle("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    public View getContentView(){
        ViewGroup views = (ViewGroup) this.getWindow().getDecorView();
        FrameLayout content = (FrameLayout) views.findViewById(android.R.id.content);
        return content.getChildAt(0);
    }

    protected void hideKeyboard() {
        getContentView().clearFocus();
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).
                    hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }
}
