package com.casc.rfidscanner.view;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.fragment.R6Fragment;
import com.casc.rfidscanner.utils.CommonUtils;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class R6CardView extends CardView {

    private static final String TAG = R6CardView.class.getSimpleName();

    // Constant for InnerHandler message.what
    private static final int MSG_BIND_BUCKETS = 0;
    private static final int MSG_CLEAR_BUCKETS = 1;
    private static final int MSG_ADD_BUCKET = 2;
    private static final int MSG_REMOVE_BUCKET = 3;

    private NumberSwitcher mCountNs;

    private List<String> mBuckets = new LinkedList<>();

    private Handler mHandler = new InnerHandler(this);

    public R6CardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public R6CardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCountNs(NumberSwitcher countNs) {
        this.mCountNs = countNs;
        mCountNs.setNumber(0);
    }

    public void bindData(List<String> buckets) {
        mBuckets = buckets;
        Message.obtain(mHandler, MSG_BIND_BUCKETS).sendToTarget();
    }

    public List<String> getAll() {
        return mBuckets;
    }

    public boolean contain(String epcStr) {
        return mBuckets.contains(epcStr);
    }

    public boolean isEmpty() {
        return mBuckets.isEmpty();
    }

    public boolean add(String epcStr) {
        if (!mBuckets.contains(epcStr)) {
            mBuckets.add(epcStr);
            Message.obtain(mHandler, MSG_ADD_BUCKET).sendToTarget();
            return true;
        }
        return false;
    }

    public String get(int index) {
        return mBuckets.get(index);
    }

    public String remove(int index) {
        String bucket = mBuckets.remove(index);
        Message.obtain(mHandler, MSG_REMOVE_BUCKET).sendToTarget();
        return bucket;
    }

    public void clear() {
        mBuckets.clear();
        Message.obtain(mHandler, MSG_CLEAR_BUCKETS).sendToTarget();
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R6CardView> mOuter;

        InnerHandler(R6CardView fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R6CardView outer = mOuter.get();
            switch (msg.what) {
                case MSG_BIND_BUCKETS:
                    outer.mCountNs.setNumber(outer.mBuckets.size());
                    break;
                case MSG_CLEAR_BUCKETS:
                    outer.setCardBackgroundColor(CommonUtils.getColor(R.color.snow));
                    outer.mCountNs.setNumber(0);
                    break;
                case MSG_ADD_BUCKET:
                    outer.mCountNs.increaseNumber();
                    break;
                case MSG_REMOVE_BUCKET:
                    outer.mCountNs.decreaseNumber();
                    break;
            }
            outer.setVisibility(outer.mBuckets.isEmpty() ? INVISIBLE : VISIBLE);
        }
    }
}
