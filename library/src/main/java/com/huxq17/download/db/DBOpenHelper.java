package com.huxq17.download.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.provider.Provider;

public class DBOpenHelper extends SQLiteOpenHelper {
    public DBOpenHelper(Context context) {
        super(context, "pump.db", null, 4);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //version==3
//        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.DownloadTable.TABLE_NAME + " ("
//                + Provider.DownloadTable.URL + " CHAR,"
//                + Provider.DownloadTable.PATH + " CHAR,"
//                + Provider.DownloadTable.THREAD_NUM + " INTEGER,"
//                + Provider.DownloadTable.FILE_LENGTH + " INTEGER,"
//                + Provider.DownloadTable.FINISHED + " INTEGER,"
//                + Provider.DownloadTable.CREATE_TIME + " INTEGER,"
//                + Provider.DownloadTable.TAG + " CHAR,"
//                + "primary key(" + Provider.DownloadTable.URL + "," + Provider.DownloadTable.PATH + ")"
//                + ");");

        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.DownloadTable.TABLE_NAME + " ("
                + Provider.DownloadTable.URL + " CHAR,"
                + Provider.DownloadTable.PATH + " CHAR,"
                + Provider.DownloadTable.THREAD_NUM + " INTEGER,"
                + Provider.DownloadTable.FILE_LENGTH + " INTEGER,"
                + Provider.DownloadTable.FINISHED + " INTEGER,"
                + Provider.DownloadTable.CREATE_TIME + " INTEGER,"
                + Provider.DownloadTable.TAG + " CHAR,"
                + Provider.DownloadTable.ID + " CHAR primary key);");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + Provider.CacheTable.TABLE_NAME + " ("
                + Provider.CacheTable.URL + " CHAR primary key,"
                + Provider.CacheTable.ETAG + " CHAR,"
                + Provider.CacheTable.LAST_MODIFIED + " CHAR"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            if (newVersion == 2) {
                onCreate(db);
            } else if (newVersion == 3) {
                db.execSQL("ALTER TABLE " + Provider.DownloadTable.TABLE_NAME + " ADD COLUMN " + Provider.DownloadTable.TAG + " CHAR default('');");
                onCreate(db);
            } else if (newVersion == 4) {
                if (oldVersion < 3) {
                    db.execSQL("ALTER TABLE " + Provider.DownloadTable.TABLE_NAME + " ADD COLUMN " + Provider.DownloadTable.TAG + " CHAR default('');");
                }
                String tempTable = Provider.DownloadTable.TABLE_NAME + "_temp";
                //新建临时下载表
                db.execSQL("CREATE TABLE IF NOT EXISTS " + tempTable + " ("
                        + Provider.DownloadTable.URL + " CHAR,"
                        + Provider.DownloadTable.PATH + " CHAR,"
                        + Provider.DownloadTable.THREAD_NUM + " INTEGER,"
                        + Provider.DownloadTable.FILE_LENGTH + " INTEGER,"
                        + Provider.DownloadTable.FINISHED + " INTEGER,"
                        + Provider.DownloadTable.CREATE_TIME + " INTEGER,"
                        + Provider.DownloadTable.TAG + " CHAR,"
                        + Provider.DownloadTable.ID + " CHAR primary key);");
                //复制老下载表数据到临时下载表
                db.execSQL("INSERT INTO " + tempTable + " SELECT "
                        + Provider.DownloadTable.URL + ","
                        + Provider.DownloadTable.PATH + ","
                        + Provider.DownloadTable.THREAD_NUM + ","
                        + Provider.DownloadTable.FILE_LENGTH + ","
                        + Provider.DownloadTable.FINISHED + ","
                        + Provider.DownloadTable.CREATE_TIME + ","
                        + Provider.DownloadTable.TAG + ","
                        + Provider.DownloadTable.URL
                        + " FROM " + Provider.DownloadTable.TABLE_NAME + ";");
                //删除老下载表
                db.execSQL(String.format("DROP TABLE %s;", Provider.DownloadTable.TABLE_NAME));
                //重建老下载表，并把临时下载表数据copy过去
                db.execSQL(String.format("CREATE TABLE %s AS SELECT * FROM %s", Provider.DownloadTable.TABLE_NAME, tempTable));
                //删除临时下载表
                db.execSQL(String.format("DROP TABLE %s", tempTable));
                onCreate(db);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}