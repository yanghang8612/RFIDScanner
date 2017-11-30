package com.casc.rfidscanner.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.casc.rfidscanner.helper.DBOpenHelper;

public class BaseDao {
    private DBOpenHelper dbOpenHelper;
    private SQLiteDatabase db;

    private String tableName;

    public BaseDao(String tableName, Context context) {
        this.tableName = tableName;
        this.dbOpenHelper = new DBOpenHelper(context);
    }

    protected long insertContentValues(ContentValues values) {
        //获取SQLiteDatabase实例
        db = dbOpenHelper.getWritableDatabase();
        //插入数据库中
        long result = db.insert(tableName, null, values);
        close();
        return result;
    }

    protected Cursor simpleQuery() {
        db = dbOpenHelper.getReadableDatabase();
        //获取Cursor
        Cursor c = db.query(tableName, null, null, null, null, null, null, null);
//        close();
        return c;
    }

    protected Cursor getByIdCursor(int id) {
        db = dbOpenHelper.getReadableDatabase();
        //获取Cursor
        Cursor c = db.query(tableName, null, "_id=?", new String[]{String.valueOf(id)}, null, null, null, null);
//        close();
        return c;
    }

    protected int deleteByid(int id) {
        db = dbOpenHelper.getWritableDatabase();
        int result = db.delete(tableName, "_id=?", new String[]{String.valueOf(id)});
        close();
        return result;
    }


    //更新数据库的内容
    protected int update(ContentValues values, String whereClause, String[] whereArgs) {
        db = dbOpenHelper.getWritableDatabase();
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

    //关闭数据库
    public void close() {
        if (db != null) {
            db.close();
        }
    }

}
