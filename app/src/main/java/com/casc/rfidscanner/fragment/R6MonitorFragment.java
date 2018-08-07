package com.casc.rfidscanner.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.baidu.tts.client.SpeechSynthesizer;
import com.casc.rfidscanner.MyApplication;
import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.R;
import com.casc.rfidscanner.activity.DeliveryDetailActivity;
import com.casc.rfidscanner.activity.MainActivity;
import com.casc.rfidscanner.adapter.ClientAdapter;
import com.casc.rfidscanner.adapter.DMBillAdapter;
import com.casc.rfidscanner.adapter.HintAdapter;
import com.casc.rfidscanner.backend.HttpServer;
import com.casc.rfidscanner.backend.InsHandler;
import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.Client;
import com.casc.rfidscanner.bean.DeliveryBill;
import com.casc.rfidscanner.bean.Hint;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageBillBucket;
import com.casc.rfidscanner.helper.param.MessageBillComplete;
import com.casc.rfidscanner.helper.param.MessageBillDelivery;
import com.casc.rfidscanner.helper.param.MessageBucket;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillFinishedMessage;
import com.casc.rfidscanner.message.BillStoredMessage;
import com.casc.rfidscanner.message.BillUpdatedMessage;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.NewClientMessage;
import com.casc.rfidscanner.utils.ActivityCollector;
import com.casc.rfidscanner.utils.CommonUtils;
import com.chad.library.adapter.base.BaseQuickAdapter;

import org.greenrobot.eventbus.EventBus;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class R6MonitorFragment extends BaseFragment implements InsHandler {

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
        DeliveryBill bill = TextUtils.isEmpty(message.getBill()) ?
                new DeliveryBill(CommonUtils.hexToBytes(message.getCard())) :
                new DeliveryBill(CommonUtils.hexToBytes(message.getCard()),
                        CommonUtils.hexToBytes(message.getBill()));
        if (!mBillsMap.containsKey(bill.getCardID())) {
            mBills.add(bill);
            mBillsMap.put(bill.getCardID(), bill);
        } else {
            bill = mBillsMap.get(bill.getCardID());
        }
        bill.setClient(message.getClient());
        for (MessageBucket bucket : message.getBuckets()) {
            bill.addBucket(new Bucket(bucket.getEPC(), bucket.getTime()));
        }
        EventBus.getDefault().post(new BillUpdatedMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageBillBucket message) {
        DeliveryBill bill = mBillsMap.get(message.getCardID());
        if (message.isRemoved())
            bill.removeBucket(message.getBucketEPC());
        else
            bill.addBucket(message.getBucketEPC());
        EventBus.getDefault().post(new BillUpdatedMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageBillComplete message) {
        mBillsMap.get(message.getCardID()).setComplete(true);
        EventBus.getDefault().post(new BillUpdatedMessage());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(BillFinishedMessage message) {
        DeliveryBill bill = MyVars.deliveryBillToShow;
        // 异步上传平台
        final MessageDelivery delivery =
                new MessageDelivery(
                        TextUtils.isEmpty(bill.getBillID()) ? "0000000000" : bill.getBillID(),
                        (char) (TextUtils.isEmpty(bill.getBillID()) ? 2 : bill.checkBill() ?  0 : 1),
                        message.dealer,
                        message.driver);
        for (Bucket bucket : bill.getBuckets()) {
            delivery.addBucket(bucket.getTime() / 1000, CommonUtils.bytesToHex(bucket.getEpc()));
        }
        NetHelper.getInstance().uploadDeliveryMessage(delivery).enqueue(new Callback<Reply>() {
            @Override
            public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                Reply body = response.body();
                if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                    MyVars.cache.storeDeliveryBill(delivery);
                    EventBus.getDefault().post(new BillStoredMessage());
                } else {
                    EventBus.getDefault().post(new BillUploadedMessage(false));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                MyVars.cache.storeDeliveryBill(delivery);
                EventBus.getDefault().post(new BillStoredMessage());
            }
        });
        mBills.remove(bill);
        mBillsMap.remove(CommonUtils.bytesToHex(bill.getCard()));
        mBillAdapter.notifyDataSetChanged();
    }

    @Override
    protected void initFragment() {
        mMonitorStatusLl.setVisibility(View.GONE);
        mReaderStatusLl.setVisibility(View.GONE);

        mClientAdapter = new ClientAdapter(mClients);
        mClientListRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mClientListRv.setAdapter(mClientAdapter);

        mBillAdapter = new DMBillAdapter(mBills);
        mBillAdapter.setOnItemClickListener(new BaseQuickAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
                if (ActivityCollector.getTopActivity() instanceof MainActivity) {
                    MyVars.deliveryBillToShow = mBills.get(position);
                    DeliveryDetailActivity.actionStart(getContext());
                }
            }
        });
        mBillListRv.setLayoutManager(new LinearLayoutManager(getContext()));
        mBillListRv.setAdapter(mBillAdapter);

        MyVars.fragmentExecutor.scheduleWithFixedDelay(new ClientStatusCheckTask(), 0, 1, TimeUnit.SECONDS);

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

    @Override
    public void sensorSignal(boolean isHigh) {
    }

    @Override
    public void dealIns(byte[] ins) {
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
                    break;
            }
        }
    }

    private class ClientStatusCheckTask implements Runnable {

        @Override
        public void run() {
            mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_UPDATE_CLIENTS), 1000);
        }
    }
}
