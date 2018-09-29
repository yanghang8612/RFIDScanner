package com.casc.rfidscanner.backend;

import android.support.annotation.NonNull;
import android.util.Pair;

import com.casc.rfidscanner.MyParams;
import com.casc.rfidscanner.MyVars;
import com.casc.rfidscanner.bean.LinkType;
import com.casc.rfidscanner.dao.MessageDao;
import com.casc.rfidscanner.helper.ConfigHelper;
import com.casc.rfidscanner.helper.DBHelper;
import com.casc.rfidscanner.helper.NetHelper;
import com.casc.rfidscanner.helper.param.MessageCommon;
import com.casc.rfidscanner.helper.param.MessageDealer;
import com.casc.rfidscanner.helper.param.MessageDelivery;
import com.casc.rfidscanner.helper.param.MessageReflux;
import com.casc.rfidscanner.helper.param.MessageStack;
import com.casc.rfidscanner.helper.param.Reply;
import com.casc.rfidscanner.message.BillUploadedMessage;
import com.casc.rfidscanner.message.TagCountChangedMessage;
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

    private final MessageDao tagDao;

    private final MessageDao stackDao;

    private final MessageDao deliveryDao;

    private final MessageDao refluxDao;

    private final MessageDao dealerDao;

    private int scannedCount = 0, uploadCount = 0;

    public TagCache() {
        this.tagDao = new MessageDao(DBHelper.TABLE_NAME_TAG_MESSAGE);
        this.stackDao = new MessageDao(DBHelper.TABLE_NAME_STACK_MESSAGE);
        this.deliveryDao = new MessageDao(DBHelper.TABLE_NAME_DELIVERY_MESSAGE);
        this.refluxDao = new MessageDao(DBHelper.TABLE_NAME_REFLUX_MESSAGE);
        this.dealerDao = new MessageDao(DBHelper.TABLE_NAME_DEALER_MESSAGE);
        MyVars.executor.scheduleWithFixedDelay(new LifecycleCheckTask(), 0, 100, TimeUnit.MILLISECONDS);
        MyVars.executor.scheduleWithFixedDelay(new StoredUploadTask(), 3000, 500, TimeUnit.MILLISECONDS); // 延迟5秒开始，便于界面有时间显示
    }

    public synchronized long getStoredCount() {
        return tagDao.rowCount();
    }

    public synchronized void clear() {
        cache.clear();
        scannedCount = uploadCount = 0;
    }

    public synchronized boolean insert(String epc) {
        if (!cache.containsKey(epc)) {
            scannedCount += 1;
            cache.put(epc, new Tag(epc));
            cache.get(epc).status = TagStatus.NONE;
            return true;
        }
        return false;
    }

    public synchronized void setTID(byte[] command) {
        String tid = CommonUtils.bytesToHex(Arrays.copyOfRange(command, 6 + command[5], command.length - 2));
        String epc = CommonUtils.bytesToHex(Arrays.copyOfRange(command, 8, 6 + command[5]));
        Tag tag = cache.get(epc);
        if (tag != null && tag.tid.isEmpty()) {
            cache.get(epc).tid = tid;
            cache.get(epc).status = TagStatus.UPLOADING;
            upload(tid, epc);
        }
    }

    public synchronized void storeStackMessage(MessageStack stack) {
        stackDao.insert(CommonUtils.toJson(stack));
    }

    public synchronized int getStoredStackMessageCount() {
        return stackDao.rowCount();
    }

    public synchronized void storeDeliveryBill(MessageDelivery delivery) {
        deliveryDao.insert(CommonUtils.toJson(delivery));
    }

    public synchronized int getStoredDeliveryBillCount() {
        return deliveryDao.rowCount();
    }

    public synchronized void storeRefluxBill(MessageReflux reflux) {
        refluxDao.insert(CommonUtils.toJson(reflux));
    }

    public synchronized int getStoredRefluxBillCount() {
        return refluxDao.rowCount();
    }

    public synchronized void storeDealerBill(MessageDealer dealer) {
        dealerDao.insert(CommonUtils.toJson(dealer));
    }

    public synchronized int getStoredDealerBillCount() {
        return dealerDao.rowCount();
    }

    private void upload(String tid, final String epc) {
        final MessageCommon common = new MessageCommon();
        common.addBucket(tid, epc);
        if (MyVars.status.canSendRequest() && tagDao.rowCount() == 0) {
            NetHelper.getInstance().uploadCommonMessage(common)
                    .enqueue(new Callback<Reply>() {
                        @Override
                        public void onResponse(@NonNull Call<Reply> call, @NonNull Response<Reply> response) {
                            Reply body = response.body();
                            if (!response.isSuccessful() || body == null || body.getCode() != 200) {
                                synchronized (TagCache.this) {
                                    tagDao.insert(CommonUtils.toJson(common));
                                }
                                cache.get(epc).status = TagStatus.STORED;
                            }
                            else {
                                uploadCount += 1;
                                cache.get(epc).status = TagStatus.UPLOADED;
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<Reply> call, @NonNull Throwable t) {
                            synchronized (TagCache.this) {
                                tagDao.insert(CommonUtils.toJson(common));
                            }
                            cache.get(epc).status = TagStatus.STORED;
                        }
                    });
        } else {
            synchronized (TagCache.this) {
                tagDao.insert(CommonUtils.toJson(common));
            }
        }
    }

    private enum TagStatus {
        NONE, UPLOADING, UPLOADED, STORED
    }

    private class Tag {

        String tid;
        String epc;
        long time;
        TagStatus status;

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
                EventBus.getDefault().post(new TagCountChangedMessage(scannedCount, uploadCount, tagDao.rowCount()));
                long lifecycle = ConfigHelper.getInt(MyParams.S_TAG_LIFECYCLE) * 60 * 1000;
                Iterator<Map.Entry<String, Tag>> it = cache.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Tag> item = it.next();
                    if (item.getValue().status == TagStatus.NONE &&
                            System.currentTimeMillis() - item.getValue().time > 1000) {
                        item.getValue().status = TagStatus.UPLOADING;
                        upload(item.getValue().tid, item.getKey());
                    }
                    if (System.currentTimeMillis() - item.getValue().time > lifecycle) {
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
                switch (LinkType.getType()) {
                    case R3:
                    case R4:
                    case R7:
                        if (MyVars.status.canSendRequest() && tagDao.rowCount() != 0) {
                            final Pair<Integer, String> tagPair = tagDao.findOne();
                            try {
                                Response<Reply> response = NetHelper.getInstance()
                                        .uploadCommonMessage(
                                                new Gson().fromJson(tagPair.second, MessageCommon.class))
                                        .execute();
                                Reply body = response.body();
                                if (response.isSuccessful() && body != null && body.getCode() == 200) {
                                    uploadCount += 1;
                                    tagDao.deleteById(tagPair.first);
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R5:
                        if (MyVars.status.canSendRequest() && stackDao.rowCount() != 0) {
                            final Pair<Integer, String> stackPair = stackDao.findOne();
                            try {
                                Response<Reply> response = NetHelper.getInstance()
                                        .uploadStackMessage(
                                                new Gson().fromJson(stackPair.second, MessageStack.class))
                                        .execute();
                                Reply body = response.body();
                                if (response.isSuccessful() && body != null && body.getCode() == 200) {
                                    stackDao.deleteById(stackPair.first);
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R2:
                        if (MyVars.status.canSendRequest() && refluxDao.rowCount() != 0) {
                            final Pair<Integer, String> refluxPair = refluxDao.findOne();
                            try {
                                Response<Reply> response = NetHelper.getInstance()
                                        .uploadRefluxMessage(
                                                new Gson().fromJson(refluxPair.second, MessageReflux.class))
                                        .execute();
                                Reply body = response.body();
                                if (response.isSuccessful() && body != null && body.getCode() == 200) {
                                    refluxDao.deleteById(refluxPair.first);
                                    EventBus.getDefault().post(new BillUploadedMessage(true));
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case R6:
                        if (MyVars.status.canSendRequest() && deliveryDao.rowCount() != 0) {
                            final Pair<Integer, String> deliveryPair = deliveryDao.findOne();
                            try {
                                Response<Reply> response = NetHelper.getInstance()
                                        .uploadDeliveryMessage(
                                                new Gson().fromJson(deliveryPair.second, MessageDelivery.class))
                                        .execute();
                                Reply body = response.body();
                                if (response.isSuccessful() && body != null && body.getCode() == 200) {
                                    deliveryDao.deleteById(deliveryPair.first);
                                    EventBus.getDefault().post(new BillUploadedMessage(true));
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                    case RN:
                        if (MyVars.status.canSendRequest() && dealerDao.rowCount() != 0) {
                            final Pair<Integer, String> dealer = dealerDao.findOne();
                            try {
                                Response<Reply> response = NetHelper.getInstance()
                                        .uploadDealerMessage(new Gson().fromJson(dealer.second, MessageDealer.class))
                                        .execute();
                                Reply body = response.body();
                                if (response.isSuccessful() && body != null && body.getCode() == 200) {
                                    dealerDao.deleteById(dealer.first);
                                    EventBus.getDefault().post(new BillUploadedMessage(true));
                                }
                            } catch (IOException ignored) {}
                        }
                        break;
                }
            }
        }
    }
}
