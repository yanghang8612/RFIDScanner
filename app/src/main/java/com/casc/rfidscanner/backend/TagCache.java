package com.casc.rfidscanner.backend;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.dao.BaseDao;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.DBHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageCommon;
import com.casc.rfidscanner.helper.param.MessageDealer;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.TagStoredMessage;
import com.casc.rfidscanner.message.TagUploadedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TagCache {

    private static final String TAG = TagCache.class.getSimpleName();

    // 缓存map，key为epc，value为Tag类型的实例
    private final Map<String, Tag> cache = new HashMap<>();

    private final BaseDao tagDao;

    private final BaseDao deliveryDao;

    private final BaseDao refluxDao;

    private final BaseDao dealerDao;

    private final BaseDao loginDao;

    public TagCache(Context context) {
        this.tagDao = new BaseDao(DBHelper.TABLE_NAME_TAG, context);
        this.deliveryDao = new BaseDao(DBHelper.TABLE_NAME_DELIVERY, context);
        this.refluxDao = new BaseDao(DBHelper.TABLE_NAME_REFLUX, context);
        this.dealerDao = new BaseDao(DBHelper.TABLE_NAME_DEALER, context);
        this.loginDao = new BaseDao(DBHelper.TABLE_NAME_LOGIN, context);
        MyVars.executor.scheduleWithFixedDelay(new LifecycleCheckTask(), 0, 100, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new StoredUploadTask(), 3000, 500, TimeUnit.MILLISECONDS); // 延迟5秒开始，便于界面有时间显示
    }

    public synchronized long getStoredCount() {
        return tagDao.count();
    }

    public void clear() {
        cache.clear();
    }

    public synchronized boolean insert(String epc) {
        if (!cache.containsKey(epc)) {
            cache.put(epc, new Tag(epc));
            return true;
        }
        else
            return false;
    }

    public synchronized void setTID(byte[] command) {
        String tid = CommonUtils.bytesToHex(Arrays.copyOfRange(command, 6 + command[5], command.length - 2));
        String epc = CommonUtils.bytesToHex(Arrays.copyOfRange(command, 8, 6 + command[5]));
        Tag tag = cache.get(epc);
        if (tag != null && tag.tid.isEmpty()) {
            Log.i(TAG, "tid " + tid);
            cache.get(epc).tid = tid;
            cache.get(epc).isUploaded = true;
            upload(tid, epc);
        }
    }

    public synchronized long getStoredDeliveryBillCount() {
        return deliveryDao.count();
    }

    public synchronized void storeDeliveryBill(MessageDelivery bill) {
        deliveryDao.save(new Gson().toJson(bill));
    }

    public synchronized long getStoredRefluxBill() {
        return refluxDao.count();
    }

    public synchronized void storeRefluxBill(MessageReflux bill) {
        refluxDao.save(new Gson().toJson(bill));
    }

    public synchronized long getStoredDealerBill() {
        return dealerDao.count();
    }

    public synchronized void storeDealerBill(MessageDealer bill) {
        dealerDao.save(new Gson().toJson(bill));
    }

    public synchronized void storeLoginInfo(String login) {
        loginDao.save(login);
    }

    private void upload(String tid, String epc) {
        final MessageCommon common = new MessageCommon();
        common.addBucket(tid, epc);
        NetHelper.getInstance().uploadCommonMessage(common)
                .enqueue(new Callback<Reply>() {
                    @Override
                    public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                        Reply body = response.body();
                        if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                            synchronized (TagCache.this) {
                                tagDao.save(new Gson().toJson(common));
                            }
                            EventBus.getDefault().post(new TagStoredMessage());
                        }
                        else
                            EventBus.getDefault().post(new TagUploadedMessage(false));
                    }

                    @Override
                    public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                        synchronized (TagCache.this) {
                            tagDao.save(new Gson().toJson(common));
                        }
                        EventBus.getDefault().post(new TagStoredMessage());
                    }
                });
    }

    private class Tag {

        String tid;
        String epc;
        long time;
        boolean isUploaded;

        Tag(String epc) {
            this.tid = "";
            this.epc = epc;
            this.time = System.currentTimeMillis();
        }
    }

    class LifecycleCheckTask implements Runnable {

        @Override
        public void run() {
            synchronized (TagCache.this) {
                long lifecycle = Long.valueOf(
                        ConfigHelper.getParam(MyParams.S_TAG_LIFECYCLE)
                                .replace("Min", "")) * 60 * 1000;

                Iterator<Map.Entry<String, Tag>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Tag> item = it.next();
                    if (!item.getValue().isUploaded &&
                            System.currentTimeMillis() - item.getValue().time > MyParams.READ_TID_MAX_WAIT_TIME) {
                        item.getValue().isUploaded = true;
                        upload(item.getValue().tid, item.getKey());
                    }
                    if (System.currentTimeMillis() - item.getValue().time > lifecycle) {
                        Log.i(TAG, "Remove tag:" + item.getKey());
                        it.remove();
                    }
                }
            }
        }
    }

    private class StoredUploadTask implements Runnable {

        @Override
        public void run() {
            synchronized (TagCache.this) {
                //Log.i(TAG, String.valueOf(deliveryDao.count()));
                if (tagDao.count() != 0) {
                    final Pair<Integer, String> tag = tagDao.findOne();
                    try {
                        Response<Reply> response = NetHelper.getInstance()
                                .uploadCommonMessage(new Gson().fromJson(tag.second, MessageCommon.class))
                                .execute();
                        Reply body = response.body();
                        if (response.isSuccessful() && body != null && body.getCode() == 200) {
                            tagDao.delete(tag.first);
                            EventBus.getDefault().post(new TagUploadedMessage(true));
                        }
                    } catch (IOException ignored) {}
                }
                if (deliveryDao.count() != 0) {
                    final Pair<Integer, String> delivery = deliveryDao.findOne();
                    try {
                        Response<Reply> response = NetHelper.getInstance()
                                .uploadDeliveryMessage(new Gson().fromJson(delivery.second, MessageDelivery.class))
                                .execute();
                        Reply body = response.body();
                        if (response.isSuccessful() && body != null && body.getCode() == 200) {
                            deliveryDao.delete(delivery.first);
                            EventBus.getDefault().post(new BillUploadedMessage(true));
                        }
                    } catch (IOException ignored) {}
                }
                if (refluxDao.count() != 0) {
                    final Pair<Integer, String> reflux = refluxDao.findOne();
                    try {
                        Response<Reply> response = NetHelper.getInstance()
                                .uploadRefluxMessage(new Gson().fromJson(reflux.second, MessageReflux.class))
                                .execute();
                        Reply body = response.body();
                        if (response.isSuccessful() && body != null && body.getCode() == 200) {
                            refluxDao.delete(reflux.first);
                            EventBus.getDefault().post(new BillUploadedMessage(true));
                        }
                    } catch (IOException ignored) {}
                }
                if (dealerDao.count() != 0) {
                    final Pair<Integer, String> dealer = dealerDao.findOne();
                    try {
                        Response<Reply> response = NetHelper.getInstance()
                                .uploadDealerMessage(new Gson().fromJson(dealer.second, MessageDealer.class))
                                .execute();
                        Reply body = response.body();
                        if (response.isSuccessful() && body != null && body.getCode() == 200) {
                            dealerDao.delete(dealer.first);
                            EventBus.getDefault().post(new BillUploadedMessage(true));
                        }
                    } catch (IOException ignored) {}
                }
                if (loginDao.count() != 0) {
                    final Pair<Integer, String> login = loginDao.findOne();
                    try {
                        Response<Reply> response = NetHelper.getInstance()
                                .uploadAdminLoginInfo(CommonUtils.generateRequestBody(login.second))
                                .execute();
                        Reply body = response.body();
                        if (response.isSuccessful() && body != null && body.getCode() == 200) {
                            loginDao.delete(login.first);
                        }
                    } catch (IOException ignored) {}
                }
            }
        }
    }
}
