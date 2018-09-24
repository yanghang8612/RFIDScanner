package com.casc.rfidscanner.fragment;

import android.os.Handler;
import android.os.Message;
import android.view.View;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.bean.DeliveryBill;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 空桶回流监控Fragment
 */
public class R2MonitorFragment extends BaseFragment {

    private static final String TAG = R2MonitorFragment.class.getSimpleName();
    private static final int READ_MAX_TRY_COUNT = 5;
    private static final int WRITE_MAX_TRY_COUNT = 3;
    private static final int CAN_REGISTER_READ_COUNT = 20;
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_HINT = 0;
    private static final int MSG_SUCCESS = 1;
    private static final int MSG_FAILED = 2;

    private List<DeliveryBill> bills = new ArrayList<>();

    private Map<String, DeliveryBill> billsMap = new HashMap<>();

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.GONE);
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6_monitor;
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R2MonitorFragment> mOuter;

        InnerHandler(R2MonitorFragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R2MonitorFragment outer = mOuter.get();
            switch (msg.what) {
            }
        }
    }
}
