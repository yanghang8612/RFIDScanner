package com.casc.rfidscanner.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.adapter.ClientAdapter;
import com.casc.rfidscanner.backend.HttpServer;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Client;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.helper.param.MessageBillBucket;
import com.casc.rfidscanner.helper.param.MessageBillComplete;
import com.casc.rfidscanner.helper.param.MessageBillDelivery;
import com.casc.rfidscanner.helper.param.MessageBucket;
import com.casc.rfidscanner.message.NewClientMessage;
import com.casc.rfidscanner.utils.CommonUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;

/**
 * 成品水出库监控Fragment
 */
public class R6MonitorFragment extends BaseFragment {

    private static final String TAG = R6MonitorFragment.class.getSimpleName();
    // Constant for InnerHandler message.what
    private static final int MSG_UPDATE_CLIENTS = 0;

    @BindView(R.id.rv_r6_monitor_client_list) RecyclerView mClientListRv;

    private List<DeliveryBill> mBills = new ArrayList<>();

    private Map<String, DeliveryBill> mBillsMap = new HashMap<>();

    // 客户端列表
    private List<Client> mClients = new ArrayList<>();

    // 提示消息列表适配器
    private ClientAdapter mClientAdapter;

    // Fragment内部handler
    private Handler mHandler = new InnerHandler(this);

    private HttpServer mServer = new HttpServer(8888);

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(NewClientMessage message) {
        mClients.add(message.client);
        mClientAdapter.notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageBillDelivery message) {
        Client client = message.getClient();
        DeliveryBill bill = TextUtils.isEmpty(message.getBill()) ?
                new DeliveryBill(CommonUtils.hexToBytes(message.getCard())) :
                new DeliveryBill(CommonUtils.hexToBytes(message.getCard()),
                        CommonUtils.hexToBytes(message.getBill()));
        if (!mBillsMap.containsKey(bill.getCardID())) {
            mBills.add(bill);
            mBillsMap.put(bill.getCardID(), bill);
            client.addBill(bill);
        } else {
            bill = mBillsMap.get(bill.getCardID());
        }
        for (MessageBucket bucket : message.getBuckets()) {
            bill.addBucket(new Bucket(bucket.getEPC(), bucket.getTime()));
        }
        client.getAdapter().notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageBillBucket message) {
        Client client = message.getClient();
        DeliveryBill bill = mBillsMap.get(message.getCardID());
//        if (message.isRemoved())
//            bill.removeBucket(message.getBucketEPC());
//        else
//            bill.addBucket(message.getBucketEPC());
        client.moveToFirst(bill);
        client.getAdapter().notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageBillComplete message) {
        Client client = message.getClient();
        DeliveryBill bill = mBillsMap.get(message.getCardID());
        mBills.remove(bill);
        mBillsMap.remove(bill.getCardID());
        client.removeBill(bill);
        client.getAdapter().notifyDataSetChanged();
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.GONE);

        mClientAdapter = new ClientAdapter(mContext, mClients);
        mClientListRv.setLayoutManager(new LinearLayoutManager(mContext));
        mClientListRv.setAdapter(mClientAdapter);

        MyVars.fragmentExecutor.scheduleWithFixedDelay(
                new ClientStatusCheckTask(), 0, 1, TimeUnit.SECONDS);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            mServer.start();
            Log.i(TAG, "Start HttpServer at 8888");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            mServer.stop();
            Log.i(TAG, "Stop HttpServer at 8888");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.fragment_r6_monitor;
    }

    private static class InnerHandler extends Handler {

        private WeakReference<R6MonitorFragment> mOuter;

        InnerHandler(R6MonitorFragment fragment) {
            this.mOuter = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            R6MonitorFragment outer = mOuter.get();
            switch (msg.what) {
                case MSG_UPDATE_CLIENTS:
                    outer.mClientAdapter.notifyDataSetChanged();
                    //Log.i(TAG, String.valueOf(outer.mClientListRv.getChildCount()));
                    break;
            }
        }
    }

    private class ClientStatusCheckTask implements Runnable {

        @Override
        public void run() {
            mHandler.sendMessage(Message.obtain(mHandler, MSG_UPDATE_CLIENTS));
        }
    }
}
