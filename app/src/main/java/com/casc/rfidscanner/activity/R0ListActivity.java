package com.casc.rfidscanner.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.RegisteredBucketAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class R0ListActivity extends BaseActivity {

    private static final String TAG = R0ListActivity.class.getSimpleName();

    public static void actionStart(Context context) {
        Intent intent = new Intent(context, R0ListActivity.class);
        context.startActivity(intent);
        ((BaseActivity) context).overridePendingTransition(R.anim.push_right_in, 0);
    }

    @BindView(R.id.rv_registered_list) RecyclerView mRegisteredListRv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_r0_list);
        ButterKnife.bind(this);
        mRegisteredListRv.setLayoutManager(new LinearLayoutManager(this));
        mRegisteredListRv.setAdapter(new RegisteredBucketAdapter(MyVars.registeredBuckets));
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.push_right_out);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int x = (int) ev.getRawX();
        int y = (int) ev.getRawY();
        if (!isTouchPointInView(findViewById(R.id.ll_r0_list_content), x, y)) {
            finish();
        }
        return super.dispatchTouchEvent(ev);
    }

    @OnClick(R.id.btn_r0_list_close)
    void onRootClicked() {
        finish();
    }
}
