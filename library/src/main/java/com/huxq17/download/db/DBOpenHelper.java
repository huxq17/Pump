package com.huxq17.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.huxq17.download.DownloadProvider;


public class DBOpenHelper extends SQLiteOpenHelper {
    public DBOpenHelper(Context context) {
        super(context, "pump.db", null, 4);
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
                + DownloadProvider.DownloadTable.CREATE_TIME + " INTEGER,"
                + DownloadProvider.DownloadTable.TAG + " CHAR,"
                + DownloadProvider.DownloadTable.ID + " CHAR primary key);");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + DownloadProvider.CacheTable.TABLE_NAME + " ("
                + DownloadProvider.CacheTable.URL + " CHAR primary key,"
                + DownloadProvider.CacheTable.ETAG + " CHAR,"
                + DownloadProvider.CacheTable.LAST_MODIFIED + " CHAR"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 0) {
            onCreate(db);
        } else {
            try {
                if (newVersion == 2) {
                    onCreate(db);
                } else if (newVersion == 3) {
                    db.execSQL("ALTER TABLE " + DownloadProvider.DownloadTable.TABLE_NAME + " ADD COLUMN " + DownloadProvider.DownloadTable.TAG + " CHAR default('');");
                    onCreate(db);
                } else if (newVersion == 4) {
                    if (oldVersion < 3) {
                        db.execSQL("ALTER TABLE " + DownloadProvider.DownloadTable.TABLE_NAME + " ADD COLUMN " + DownloadProvider.DownloadTable.TAG + " CHAR default('');");
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
                    //重建老下载表，并把临时下载表数据copy过去
                    db.execSQL(String.format("CREATE TABLE %s AS SELECT * FROM %s", DownloadProvider.DownloadTable.TABLE_NAME, tempTable));
                    //删除临时下载表
                    db.execSQL(String.format("DROP TABLE %s", tempTable));
                    onCreate(db);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}