package com.casc.rfidscanner.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBOpenHelper extends SQLiteOpenHelper {

    public static final String TAG = DBOpenHelper.class.getName();

    public final static int VERSION = 1;
    public final static String DB_NAME = "RFIDScanner.db";

    public static final String TABLE_NAME_TAG = "tagTable";
    public static final String TABLE_NAME_USER = "userTable";

    public static String CREATE_TBL_TAG = "CREATE TABLE if not exists " + TABLE_NAME_TAG + "(_id integer primary key autoincrement," +
            " tid text, rfid text, link text, longitude real, latitude real, timestamp text)";
    private static String CREATE_TBL_USER = "CREATE TABLE if not exists " + TABLE_NAME_USER + "(_id integer primary key autoincrement," +
            " name text, pwd text, type text)";

    public DBOpenHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, dbName, factory, version);
    }

    public DBOpenHelper(Context context) {
        this(context, DB_NAME, null, VERSION);
    }

    //数据库第一次创建时候调用
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "======Create Database START( DB_NAME = " + DB_NAME + ", VERSION = " + VERSION + ")");
        db.execSQL(CREATE_TBL_TAG);
        db.execSQL(CREATE_TBL_USER);
    }

    //数据库文件版本号发生变化时调用
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "======upgrade Database( DB_NAME = " + DB_NAME + ", VERSION = " + VERSION + ")");
    }

}
