package com.casc.rfidscanner.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    public static final String TAG = DBHelper.class.getName();

    private final static int VERSION = 1;
    private final static String DB_NAME = "RFIDScanner.db";

    public static final String TABLE_NAME_TAG = "tagTable";
    public static final String TABLE_NAME_DELIVERY = "deliveryTable";
    public static final String TABLE_NAME_REFLUX = "refluxTable";
    public static final String TABLE_NAME_DEALER = "dealerTable";
    public static final String TABLE_NAME_LOGIN = "loginTable";

    private static String CREATE_TBL_TAG =
            "CREATE TABLE if not exists " + TABLE_NAME_TAG + "(_id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_DELIVERY =
            "CREATE TABLE if not exists " + TABLE_NAME_DELIVERY + "(_id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_REFLUX =
            "CREATE TABLE if not exists " + TABLE_NAME_REFLUX + "(_id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_DEALER =
            "CREATE TABLE if not exists " + TABLE_NAME_DEALER + "(_id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_LOGIN =
            "CREATE TABLE if not exists " + TABLE_NAME_LOGIN + "(_id integer primary key autoincrement, content text)";

    private static DBHelper mInstance = null;

    private DBHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, dbName, factory, version);
    }

    private DBHelper(Context context) {
        this(context, DB_NAME, null, VERSION);
    }

    public synchronized static DBHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DBHelper(context);
        }
        return mInstance;
    }

    //数据库第一次创建时候调用
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "======Create Database START( DB_NAME = " + DB_NAME + ", VERSION = " + VERSION + ")");
        db.execSQL(CREATE_TBL_TAG);
        db.execSQL(CREATE_TBL_DELIVERY);
        db.execSQL(CREATE_TBL_REFLUX);
        db.execSQL(CREATE_TBL_DEALER);
        db.execSQL(CREATE_TBL_LOGIN);
    }

    //数据库文件版本号发生变化时调用
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "======upgrade Database( DB_NAME = " + DB_NAME + ", VERSION = " + VERSION + ")");
    }

}
