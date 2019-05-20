package com.casc.rfidscanner.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.casc.rfidscanner.MyApplication;

public class DBHelper extends SQLiteOpenHelper {

    public static final String TAG = DBHelper.class.getName();

    private final static int VERSION = 1;
    private final static String DB_NAME = "RFIDScanner.db";

    public static final String TABLE_NAME_LOG_MESSAGE = "logMessageTable";
    public static final String TABLE_NAME_TAG_MESSAGE = "tagMessageTable";
    public static final String TABLE_NAME_STACK_MESSAGE = "stackMessageTable";
    public static final String TABLE_NAME_DELIVERY_MESSAGE = "deliveryMessageTable";
    public static final String TABLE_NAME_REFLUX_MESSAGE = "refluxMessageTable";
    public static final String TABLE_NAME_DELIVERY_BILL = "deliveryBillTable";
    public static final String TABLE_NAME_REFLUX_BILL = "refluxBillTable";
    public static final String TABLE_NAME_STACK_DETAIL = "stackDetailTable";

    private static String CREATE_TBL_LOG_MESSAGE =
            "CREATE TABLE if not exists " + TABLE_NAME_LOG_MESSAGE + "(id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_TAG_MESSAGE =
            "CREATE TABLE if not exists " + TABLE_NAME_TAG_MESSAGE + "(id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_STACK_MESSAGE =
            "CREATE TABLE if not exists " + TABLE_NAME_STACK_MESSAGE + "(id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_DELIVERY_MESSAGE =
            "CREATE TABLE if not exists " + TABLE_NAME_DELIVERY_MESSAGE + "(id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_REFLUX_MESSAGE =
            "CREATE TABLE if not exists " + TABLE_NAME_REFLUX_MESSAGE + "(id integer primary key autoincrement, content text)";
    private static String CREATE_TBL_LOGIN_MESSAGE =
            "CREATE TABLE if not exists " + TABLE_NAME_DELIVERY_BILL + "(id integer primary key autoincrement, card varchar(24), bill varchar(48), buckets text)";
    private static String CREATE_TBL_REFLUX_BILL =
            "CREATE TABLE if not exists " + TABLE_NAME_REFLUX_BILL + "(id integer primary key autoincrement, card varchar(24), buckets text)";
    private static String CREATE_TBL_STACK_DETAIL =
            "CREATE TABLE if not exists " + TABLE_NAME_STACK_DETAIL + "(id integer primary key autoincrement, content text)";

    private static DBHelper mInstance = null;

    private DBHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, dbName, factory, version);
    }

    private DBHelper(Context context) {
        this(context, DB_NAME, null, VERSION);
    }

    public synchronized static DBHelper getInstance() {
        if (mInstance == null) {
            mInstance = new DBHelper(MyApplication.getInstance());
        }
        return mInstance;
    }

    //数据库第一次创建时候调用
    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "======Create Database START( DB_NAME = " + DB_NAME + ", VERSION = " + VERSION + ")");
        db.execSQL(CREATE_TBL_LOG_MESSAGE);
        db.execSQL(CREATE_TBL_TAG_MESSAGE);
        db.execSQL(CREATE_TBL_STACK_MESSAGE);
        db.execSQL(CREATE_TBL_DELIVERY_MESSAGE);
        db.execSQL(CREATE_TBL_REFLUX_MESSAGE);
        db.execSQL(CREATE_TBL_LOGIN_MESSAGE);
        db.execSQL(CREATE_TBL_REFLUX_BILL);
        db.execSQL(CREATE_TBL_STACK_DETAIL);
    }

    //数据库文件版本号发生变化时调用
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "======upgrade Database( DB_NAME = " + DB_NAME + ", VERSION = " + VERSION + ")");
    }

}
