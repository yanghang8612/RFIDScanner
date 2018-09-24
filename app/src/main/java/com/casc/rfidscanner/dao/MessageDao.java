package com.casc.rfidscanner.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Pair;

import com.casc.rfidscanner.helper.DBHelper;

public class MessageDao {
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";

    private DBHelper dbHelper = DBHelper.getInstance();
    private String tableName;

    public MessageDao(String tableName) {
        this.tableName = tableName;
    }

    public long insert(String content) {
        return dbHelper.getWritableDatabase().insert(
                tableName, null, transform2ContentValues(content));
    }

    private ContentValues transform2ContentValues(String content) {
        return transform2ContentValues(-1, content);
    }

    private ContentValues transform2ContentValues(int id, String content) {
        ContentValues values = new ContentValues();
        if (id >= 0) values.put(COLUMN_ID, id);
        values.put(COLUMN_CONTENT, content);
        return values;
    }

    public Pair<Integer, String> findOne() {
        Pair<Integer, String> result = null;
        Cursor c = getAllRows();
        if (c != null) {
            if (c.moveToNext()) {
                int id = c.getInt(c.getColumnIndex(COLUMN_ID));
                String content = c.getString(c.getColumnIndex(COLUMN_CONTENT));
                result =  new Pair<>(id, content);
            }
            c.close();
        }
        return result;
    }

    public int updateContent(int id, String content) {
        return update(transform2ContentValues(id, content), COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    private int update(ContentValues values, String whereClause, String[] whereArgs) {
        return dbHelper.getWritableDatabase().update(
                tableName, values, whereClause, whereArgs);
    }

    public String getContentById(int id) {
        String result = null;
        Cursor c = getRowById(id);
        if (c != null) {
            if (c.moveToNext()) {
                result = c.getString(c.getColumnIndex(COLUMN_CONTENT));
            }
            c.close();
        }
        return result;
    }

    private Cursor getRowById(int id) {
        return dbHelper.getWritableDatabase().query(
                tableName, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);
    }

    private Cursor getAllRows() {
        return dbHelper.getWritableDatabase().query(
                tableName, null, null, null, null, null, null, null);
    }

    public void deleteById(int id) {
        dbHelper.getWritableDatabase().delete(
                tableName, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
    }

    public int rowCount() {
        int count = 0;
        Cursor c = getAllRows();
        if (c != null) {
            count = c.getCount();
            c.close();
        }
        return count;
    }
}
