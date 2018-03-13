package com.casc.rfidscanner.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import com.casc.rfidscanner.helper.DBHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 约定以TEXT类型的JSON串保存 缓存tag 以及 经销商列表、司机列表
 * （若仅使用特定字段则解析JSON串）
 */
public class BaseDao {
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_CONTENT = "content";

    private DBHelper dbHelper;
    private SQLiteDatabase db;

    private String tableName; // 区分

    public BaseDao(String tableName, Context context) {
        this.tableName = tableName;
        this.dbHelper = DBHelper.getInstance(context);
    }

    /**
     * 插入新数据
     */
    public long save(String content) {
        ContentValues values = transfor2ContentValues(null, content);
        return insertContentValues(values);
    }

    /**
     * 查找所有JSON数据
     */
    public List<String> findAllContent() {
        List<String> result = new ArrayList<>();
        Cursor c = simpleQuery(); // remain

        while (c.moveToNext()) {
            result.add(c.getString(c.getColumnIndex(COLUMN_CONTENT))); // JSON
        }
        c.close();
        return result;
    }

    public Pair<Integer, String> findOne() {
        Pair<Integer, String> result = null;
        Cursor c = simpleQuery(); // remain

        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex(COLUMN_ID)); // SQLite feature
            String content = c.getString(c.getColumnIndex(COLUMN_CONTENT)); // JSON
            result = new Pair<>(id, content);
        }
        c.close();
        return result;
    }

    /**
     * 查找所有JSON数据
     *
     * @return
     */
    public Map<Integer, String> findAll() {
        Map<Integer, String> result = new HashMap<>();
        Cursor c = simpleQuery(); // remain

        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndex(COLUMN_ID)); // SQLite feature
            String content = c.getString(c.getColumnIndex(COLUMN_CONTENT)); // JSON
            result.put(id, content);
        }
        c.close();
        return result;
    }

    /**
     * 按照id删除指定数据
     */
    public int delete(int id) {
        return deleteById(id);
    }

    /**
     * 修改Tag
     */
    public int update(Integer id, String content) {
        ContentValues values = transfor2ContentValues(id, content);
        return update(values, COLUMN_ID + "=?", new String[]{id.toString()});
    }

    /**
     * 通过id查找数据
     *
     * @param id
     * @return
     */
    public String getById(int id) {
        Cursor c = getByIdCursor(id);
        while (c.moveToNext()) {
            String content = c.getString(c.getColumnIndex(COLUMN_CONTENT));
            c.close();
            return content;
        }
        return null;
    }


    protected long insertContentValues(ContentValues values) {
        // 获取SQLiteDatabase实例
        db = dbHelper.getWritableDatabase();
        // 插入数据库中
        long result = db.insert(tableName, null, values);
        close();
        return result;
    }

    protected Cursor simpleQuery() {
        db = dbHelper.getReadableDatabase();
        // 获取Cursor
        Cursor c = db.query(tableName, null, null, null, null, null, null, null);
//        close();
        return c;
    }

    protected Cursor getByIdCursor(int id) {
        db = dbHelper.getReadableDatabase();
        // 获取Cursor
        Cursor c = db.query(tableName, null, COLUMN_ID + "=?", new String[]{String.valueOf(id)}, null, null, null, null);
//        close();
        return c;
    }

    protected int deleteById(int id) {
        db = dbHelper.getWritableDatabase();
        int result = db.delete(tableName, COLUMN_ID + "=?", new String[]{String.valueOf(id)});
        close();
        return result;
    }


    // 更新数据库的内容
    protected int update(ContentValues values, String whereClause, String[] whereArgs) {
        db = dbHelper.getWritableDatabase();
        int result = db.update(tableName, values, whereClause, whereArgs);
        close();
        return result;
    }

    public long count() {
        long count = 0;
        Cursor c = simpleQuery();
        while (c.moveToNext()) {
            count++;
        }
        close();
        return count;
    }

    // 关闭数据库
    public void close() {
        if (db != null) {
            db.close();
        }
    }

    public static ContentValues transfor2ContentValues(Integer id, String textarea) {
        ContentValues values = new ContentValues();
        if (id != null && id >= 0) values.put(COLUMN_ID, id);
        values.put(COLUMN_CONTENT, textarea);
        return values;
    }

}
