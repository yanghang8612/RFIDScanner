package com.casc.rfidscanner.fragment;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.view.View;

import com.casc.rfidscanner.R;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.DeliveryBill;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class R2MonitorFragment extends BaseFragment implements InsHandler {

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

    @Override
    public void sensorSignal(boolean isHigh) {

    }

    @Override
    public void dealIns(byte[] ins) {
        int command = ins[2] & 0xFF;
        switch (command) {
            default: // 其他指令（轮询或失败指令）的处理流程，因为需要操作UI使用Handler传递消息
                mHandler.sendMessage(Message.obtain(mHandler, MSG_RECEIVED_FRAME_FROM_READER, ins));
        }
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
