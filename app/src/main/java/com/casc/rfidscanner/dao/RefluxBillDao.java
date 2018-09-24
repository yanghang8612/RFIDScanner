package com.casc.rfidscanner.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.casc.rfidscanner.bean.Bucket;
import com.casc.rfidscanner.bean.RefluxBill;
import com.casc.rfidscanner.helper.DBHelper;

import java.util.ArrayList;
import java.util.List;

public class RefluxBillDao {
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CARD = "card";
    private static final String COLUMN_BUCKETS = "buckets";

    private DBHelper dbHelper = DBHelper.getInstance();
    private String tableName = DBHelper.TABLE_NAME_REFLUX_BILL;

    public long insert(RefluxBill bill) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_CARD, bill.getCardStr());
        values.put(COLUMN_BUCKETS, "");
        return dbHelper.getWritableDatabase().insert(
                tableName, null, values);
    }

    public void remove(RefluxBill bill) {
        dbHelper.getWritableDatabase().delete(
                tableName, COLUMN_CARD + "=?", new String[]{bill.getCardStr()});
    }

    public void insertBucket(String card, String bucket) {
        Cursor c = getRowByCard(card);
        if (c != null) {
            if (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndex(COLUMN_ID));
                String buckets = c.getString(c.getColumnIndex(COLUMN_BUCKETS));
                if (!TextUtils.isEmpty(buckets)) {
                    buckets += ",";
                }
                buckets += bucket;
                ContentValues values = new ContentValues();
                values.put(COLUMN_BUCKETS, buckets);
                dbHelper.getWritableDatabase().update(
                        tableName, values, "id=?", new String[]{String.valueOf(id)});
            }
            c.close();
        }
    }

    public void updateBuckets(RefluxBill bill) {
        Cursor c = getRowByCard(bill.getCardStr());
        if (c != null) {
            if (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndex(COLUMN_ID));
                StringBuilder buckets = new StringBuilder();
                for (Bucket bucket : bill.getBuckets()) {
                    buckets.append(bucket.getEpcStr() + ",");
                }
                ContentValues values = new ContentValues();
                values.put(COLUMN_BUCKETS, buckets.toString());
                dbHelper.getWritableDatabase().update(
                        tableName, values, "id=?", new String[]{String.valueOf(id)});
            }
            c.close();
        }
    }

    public List<RefluxBill> getAllBills() {
        List<RefluxBill> result = new ArrayList<>();
        Cursor c = getAllRows();
        if (c != null) {
            while (c.moveToNext()) {
                String card = c.getString(c.getColumnIndex(COLUMN_CARD));
                String[] buckets = c.getString(c.getColumnIndex(COLUMN_BUCKETS)).split(",");
                RefluxBill refluxBill = new RefluxBill(card);
                for (String bucket : buckets) {
                    refluxBill.addBucket(bucket);
                }
                result.add(refluxBill);
            }
            c.close();
        }
        return result;
    }

    private Cursor getAllRows() {
        return dbHelper.getWritableDatabase().query(
                tableName, null, null, null, null, null, null, null);
    }

    private Cursor getRowByCard(String card) {
        return dbHelper.getWritableDatabase().query(
                tableName, null, COLUMN_CARD + "=?", new String[]{card}, null, null, null, null);
    }

    public long rowCount() {
        int count = 0;
        Cursor c = getAllRows();
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        return count;
    }
}
