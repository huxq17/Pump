package com.huxq17.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.huxq17.download.DownloadProvider;


public class DBOpenHelper extends SQLiteOpenHelper {
    public DBOpenHelper(Context context) {
        super(context, "pump.db", null, 7);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //version==3
//        db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadProvider.DownloadTable.TABLE_NAME + " ("
//                + DownloadProvider.DownloadTable.URL + " CHAR,"
//                + DownloadProvider.DownloadTable.PATH + " CHAR,"
//                + DownloadProvider.DownloadTable.THREAD_NUM + " INTEGER,"
//                + DownloadProvider.DownloadTable.FILE_LENGTH + " INTEGER,"
//                + DownloadProvider.DownloadTable.FINISHED + " INTEGER,"
//                + DownloadProvider.DownloadTable.CREATE_TIME + " INTEGER,"
//                + DownloadProvider.DownloadTable.TAG + " CHAR,"
//                + "primary key(" + DownloadProvider.DownloadTable.URL + "," + DownloadProvider.DownloadTable.PATH + ")"
//                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadProvider.DownloadTable.TABLE_NAME + " ("
                + DownloadProvider.DownloadTable.URL + " CHAR,"
                + DownloadProvider.DownloadTable.PATH + " CHAR,"
                + DownloadProvider.DownloadTable.THREAD_NUM + " INTEGER,"
                + DownloadProvider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + DownloadProvider.DownloadTable.FINISHED + " INTEGER,"
                + DownloadProvider.DownloadTable.CREATE_TIME + " TIMESTAMP NOT NULL default (strftime('%s','now','localtime')*1000+(strftime('%f','now','localtime')-strftime('%S','now','localtime'))*1000),"
                + DownloadProvider.DownloadTable.TAG + " CHAR,"
                + DownloadProvider.DownloadTable.ID + " CHAR primary key,"
                + DownloadProvider.DownloadTable.SCHEMA_URI + " CHAR,"
                + DownloadProvider.DownloadTable.MIME_TYPE + " CHAR);");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadProvider.CacheTable.TABLE_NAME + " ("
                + DownloadProvider.CacheTable.URL + " CHAR primary key,"
                + DownloadProvider.CacheTable.ETAG + " CHAR,"
                + DownloadProvider.CacheTable.LAST_MODIFIED + " CHAR"
                + ");");
    }

    private void newVersion3(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 2) {
            onCreate(db);
        }
    }

    private void newVersion4(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 3) {
            newVersion3(db, oldVersion);
        }
        try {
            db.execSQL("ALTER TABLE " + DownloadProvider.DownloadTable.TABLE_NAME + " ADD COLUMN " + DownloadProvider.DownloadTable.ID + " CHAR default('');");
        } catch (SQLiteException ignore) {
        }
        String tempTable = DownloadProvider.DownloadTable.TABLE_NAME + "_temp";
        //新建临时下载表
        db.execSQL("CREATE TABLE IF NOT EXISTS " + tempTable + " ("
                + DownloadProvider.DownloadTable.URL + " CHAR,"
                + DownloadProvider.DownloadTable.PATH + " CHAR,"
                + DownloadProvider.DownloadTable.THREAD_NUM + " INTEGER,"
                + DownloadProvider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + DownloadProvider.DownloadTable.FINISHED + " INTEGER,"
                + DownloadProvider.DownloadTable.CREATE_TIME + " INTEGER,"
                + DownloadProvider.DownloadTable.TAG + " CHAR,"
                + DownloadProvider.DownloadTable.ID + " CHAR primary key);");
        //复制老下载表数据到临时下载表
        db.execSQL("INSERT INTO " + tempTable + " SELECT "
                + DownloadProvider.DownloadTable.URL + ","
                + DownloadProvider.DownloadTable.PATH + ","
                + DownloadProvider.DownloadTable.THREAD_NUM + ","
                + DownloadProvider.DownloadTable.FILE_LENGTH + ","
                + DownloadProvider.DownloadTable.FINISHED + ","
                + DownloadProvider.DownloadTable.CREATE_TIME + ","
                + DownloadProvider.DownloadTable.TAG + ","
                + DownloadProvider.DownloadTable.URL
                + " FROM " + DownloadProvider.DownloadTable.TABLE_NAME + ";");
        //删除老下载表
        db.execSQL(String.format("DROP TABLE %s;", DownloadProvider.DownloadTable.TABLE_NAME));
        //重建下载表，并把临时下载表数据copy过去
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadProvider.DownloadTable.TABLE_NAME + " ("
                + DownloadProvider.DownloadTable.URL + " CHAR,"
                + DownloadProvider.DownloadTable.PATH + " CHAR,"
                + DownloadProvider.DownloadTable.THREAD_NUM + " INTEGER,"
                + DownloadProvider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + DownloadProvider.DownloadTable.FINISHED + " INTEGER,"
                + DownloadProvider.DownloadTable.CREATE_TIME + " INTEGER,"
                + DownloadProvider.DownloadTable.TAG + " CHAR,"
                + DownloadProvider.DownloadTable.ID + " CHAR primary key);");
        db.execSQL("INSERT INTO " + DownloadProvider.DownloadTable.TABLE_NAME + " SELECT * " + " FROM " + tempTable + ";");
        //删除临时下载表
        db.execSQL(String.format("DROP TABLE %s", tempTable));
    }

    private void newVersion5(SQLiteDatabase db, int oldVersion) {
        if (oldVersion < 4) {
            newVersion4(db, oldVersion);
        }
        String tempTable = DownloadProvider.DownloadTable.TABLE_NAME + "_temp";
        db.execSQL("CREATE TABLE " + tempTable + " ("
                + DownloadProvider.DownloadTable.URL + " CHAR,"
                + DownloadProvider.DownloadTable.PATH + " CHAR,"
                + DownloadProvider.DownloadTable.THREAD_NUM + " INTEGER,"
                + DownloadProvider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + DownloadProvider.DownloadTable.FINISHED + " INTEGER,"
                + DownloadProvider.DownloadTable.CREATE_TIME + " TIMESTAMP NOT NULL default (strftime('%s','now','localtime')*1000+(strftime('%f','now','localtime')-strftime('%S','now','localtime'))*1000),"
                + DownloadProvider.DownloadTable.TAG + " CHAR,"
                + DownloadProvider.DownloadTable.ID + " CHAR primary key);");
        //复制老表数据到临时表
        db.execSQL("INSERT INTO " + tempTable + " SELECT *" + " FROM " + DownloadProvider.DownloadTable.TABLE_NAME + ";");
        //删除老下载表
        db.execSQL(String.format("DROP TABLE %s;", DownloadProvider.DownloadTable.TABLE_NAME));
        //重建下载表，并把临时下载表数据copy过去
        db.execSQL("CREATE TABLE " + DownloadProvider.DownloadTable.TABLE_NAME + " ("
                + DownloadProvider.DownloadTable.URL + " CHAR,"
                + DownloadProvider.DownloadTable.PATH + " CHAR,"
                + DownloadProvider.DownloadTable.THREAD_NUM + " INTEGER,"
                + DownloadProvider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + DownloadProvider.DownloadTable.FINISHED + " INTEGER,"
                + DownloadProvider.DownloadTable.CREATE_TIME + " TIMESTAMP NOT NULL default (strftime('%s','now','localtime')*1000+(strftime('%f','now','localtime')-strftime('%S','now','localtime'))*1000),"
                + DownloadProvider.DownloadTable.TAG + " CHAR,"
                + DownloadProvider.DownloadTable.ID + " CHAR primary key);");
        db.execSQL("INSERT INTO " + DownloadProvider.DownloadTable.TABLE_NAME + " SELECT * " + " FROM " + tempTable + ";");
        //删除临时下载表
        db.execSQL(String.format("DROP TABLE %s", tempTable));
    }

    private void newVersion6(SQLiteDatabase db, int oldVersion) {
        //add uri to support android Q download.
        if (oldVersion < 5) {
            newVersion5(db, oldVersion);
        }
        db.execSQL("ALTER TABLE " + DownloadProvider.DownloadTable.TABLE_NAME + " ADD COLUMN " + DownloadProvider.DownloadTable.SCHEMA_URI + " CHAR;");
        db.execSQL("ALTER TABLE " + DownloadProvider.DownloadTable.TABLE_NAME + " ADD COLUMN " + DownloadProvider.DownloadTable.MIME_TYPE + " CHAR;");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 0) {
            onCreate(db);
        } else {
            if (newVersion == 6) {
                newVersion6(db, oldVersion);
            } else if (newVersion == 5) {
                newVersion5(db, oldVersion);
            } else if (newVersion == 4) {
                newVersion4(db, oldVersion);
            } else if (newVersion == 3) {
                newVersion3(db, oldVersion);
            } else if (newVersion == 2) {
                onCreate(db);
            }
        }
    }

}