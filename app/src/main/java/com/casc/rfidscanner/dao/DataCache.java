package com.casc.rfidscanner.dao;

import android.util.Log;
import android.util.Pair;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.helper.SpHelper;
import com.casc.rfidscanner.helper.DBHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.net.param.MsgLine;
import com.casc.rfidscanner.helper.net.param.MsgDelivery;
import com.casc.rfidscanner.helper.net.param.MsgLog;
import com.casc.rfidscanner.helper.net.param.MsgReflux;
import com.casc.rfidscanner.helper.net.param.MsgStack;
import com.casc.rfidscanner.helper.net.param.Reply;
import com.casc.rfidscanner.message.TagUploadedMessage;
import com.casc.rfidscanner.utils.CommonUtils;
import com.google.gson.Gson;


import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Response;

public class DataCache {

    private static final String TAG = DataCache.class.getSimpleName();

    private final Map<String, Long> cache = new HashMap<>();

    private final BaseDao logDao;

    private final BaseDao tagDao;

    private final BaseDao stackDao;

    private final BaseDao deliveryDao;

    private final BaseDao refluxDao;

    public DataCache() {
        this.logDao = new BaseDao(DBHelper.TABLE_NAME_LOG_MESSAGE);
        this.tagDao = new BaseDao(DBHelper.TABLE_NAME_TAG_MESSAGE);
        this.stackDao = new BaseDao(DBHelper.TABLE_NAME_STACK_MESSAGE);
        this.deliveryDao = new BaseDao(DBHelper.TABLE_NAME_DELIVERY_MESSAGE);
        this.refluxDao = new BaseDao(DBHelper.TABLE_NAME_REFLUX_MESSAGE);
        MyVars.executor.scheduleWithFixedDelay(new LifecycleCheckTask(), 3000, 100, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new StoredUploadTask(), 3000, 500, TimeUnit.MILLISECONDS); // 延迟5秒开始，便于界面有时间显示
    }

    public synchronized long getStoredCount() {
        return tagDao.rowCount();
    }

    public synchronized void clear() {
        cache.clear();
    }

    public synchronized void storeLogMessage(MsgLog msg) {
        logDao.insert(CommonUtils.toJson(msg));
    }

    public synchronized boolean insert(String epc) {
        if (!cache.containsKey(epc)) {
            cache.put(epc, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public synchronized void storeLineMessage(MsgLine msg) {
        tagDao.insert(CommonUtils.toJson(msg));
    }

    public synchronized void storeStackMessage(MsgStack msg) {
        stackDao.insert(CommonUtils.toJson(msg));
    }

    public synchronized void storeDeliveryBill(MsgDelivery msg) {
        deliveryDao.insert(CommonUtils.toJson(msg));
    }

    public synchronized void storeRefluxBill(MsgReflux msg) {
        refluxDao.insert(CommonUtils.toJson(msg));
    }

    class LifecycleCheckTask implements Runnable {

        @Override
        public void run() {
            synchronized (DataCache.this) {
                long lifecycle = SpHelper.getInt(MyParams.S_TAG_LIFECYCLE) * 60 * 1000;
                Iterator<Map.Entry<String, Long>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Long> item = it.next();
                    if (System.currentTimeMillis() - item.getValue() > lifecycle) {
                        it.remove();
                    }
                }
            }
        }
    }

    private class StoredUploadTask implements Runnable {

        @Override
        public void run() {
            synchronized (DataCache.this) {
//                if (MyVars.status.canSendRequest() && logDao.rowCount() != 0) { // 上传暂存本地的Log信息
//                    final Pair<Integer, String> logPair = logDao.findOne();
//                    try {
//                        Response<Reply> response = NetHelper.getInstance()
//                                .uploadLogMsg(
//                                        new Gson().fromJson(logPair.second, MsgLog.class))
//                                .execute();
//                        Reply body = response.body();
//                        if (response.isSuccessful() && body != null && body.getInt() == 200) {
//                            logDao.deleteById(logPair.first);
//                        }
//                    } catch (IOException ignored) {}
//                }
                switch (LinkType.getType()) { // 根据环节的配置不同上传不同的暂存数据
                    case R4:
                        if (MyVars.status.canSendRequest() && tagDao.rowCount() != 0) { // 下线数据
                            final Pair<Integer, String> tagPair = tagDao.findOne();
                            try {
                                Reply reply = NetHelper.getInstance().uploadProductMsg(
                                        new Gson().fromJson(tagPair.second, MsgLine.class))
                                        .execute().body();
                                if (reply != null && reply.getCode() == 200) {
                                    tagDao.deleteById(tagPair.first);
                                    EventBus.getDefault().post(new TagUploadedMessage());
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R3:
                    case R7:
                        if (MyVars.status.canSendRequest() && tagDao.rowCount() != 0) { // 上线、筛选数据
                            final Pair<Integer, String> tagPair = tagDao.findOne();
                            try {
                                Reply reply = NetHelper.getInstance().uploadLineMsg(
                                        new Gson().fromJson(tagPair.second, MsgLine.class))
                                        .execute().body();
                                if (reply != null && reply.getCode() == 200) {
                                    tagDao.deleteById(tagPair.first);
                                    EventBus.getDefault().post(new TagUploadedMessage());
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R5:
                        if (MyVars.status.canSendRequest() && stackDao.rowCount() != 0) { // 打垛数据
                            final Pair<Integer, String> stackPair = stackDao.findOne();
                            try {
                                Reply reply = NetHelper.getInstance().uploadStackMsg(
                                        new Gson().fromJson(stackPair.second, MsgStack.class))
                                        .execute().body();
                                if (reply != null && reply.getCode() == 200) {
                                    stackDao.deleteById(stackPair.first);
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R2:
                        if (MyVars.status.canSendRequest() && refluxDao.rowCount() != 0) { // 回流数据
                            final Pair<Integer, String> refluxPair = refluxDao.findOne();
                            try {
                                Reply reply = NetHelper.getInstance().uploadRefluxMsg(
                                        new Gson().fromJson(refluxPair.second, MsgReflux.class))
                                        .execute().body();
                                if (reply != null && reply.getCode() == 200) {
                                    refluxDao.deleteById(refluxPair.first);
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R6:
                        if (MyVars.status.canSendRequest() && deliveryDao.rowCount() != 0) { // 出库数据
                            final Pair<Integer, String> deliveryPair = deliveryDao.findOne();
                            try {
                                Reply reply = NetHelper.getInstance().uploadDeliveryMsg(
                                        new Gson().fromJson(deliveryPair.second, MsgDelivery.class))
                                        .execute().body();
                                if (reply != null && reply.getCode() == 200) {
                                    deliveryDao.deleteById(deliveryPair.first);
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                }
            }
        }
    }
}
