package com.huxq17.download.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.huxq17.download.TransferInfo;
import com.huxq17.download.provider.Provider;

import java.util.ArrayList;
import java.util.List;

public class DBService {
    private DBOpenHelper helper;
    private static DBService instance;

    public static void init(Context context) {
        instance = new DBService(context);
    }

    private DBService(Context context) {
        helper = new DBOpenHelper(context);
    }

    public static DBService getInstance() {
        return instance;
    }

    public SQLiteDatabase getReadableDatabase() {
        return helper.getWritableDatabase();
    }

    public synchronized void updateInfo(TransferInfo downloadInfo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String querySql = "select * from " + Provider.DownloadTable.TABLE_NAME + " where " + Provider.DownloadTable.URL + "=? and "
                + Provider.DownloadTable.PATH + " =?";
        Cursor cursor = db.rawQuery(querySql, new String[]{downloadInfo.getUrl(), downloadInfo.getFilePath()});
        if (cursor.getCount() == 1) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Provider.DownloadTable.URL, downloadInfo.getUrl());
            contentValues.put(Provider.DownloadTable.PATH, downloadInfo.getFilePath());
            contentValues.put(Provider.DownloadTable.THREAD_NUM, 0);
            contentValues.put(Provider.DownloadTable.FILE_LENGTH, downloadInfo.getContentLength());
            contentValues.put(Provider.DownloadTable.FINISHED, downloadInfo.getFinished());
            db.update(Provider.DownloadTable.TABLE_NAME, contentValues,
                    Provider.DownloadTable.URL + "=? and " + Provider.DownloadTable.PATH + "=?",
                    new String[]{downloadInfo.getUrl(), downloadInfo.getFilePath()});
        } else {
            String sql = "insert into " + Provider.DownloadTable.TABLE_NAME +
                    "(" + Provider.DownloadTable.URL + ", "
                    + Provider.DownloadTable.PATH + ","
                    + Provider.DownloadTable.THREAD_NUM + ","
                    + Provider.DownloadTable.FILE_LENGTH + ","
                    + Provider.DownloadTable.FINISHED + ","
                    + Provider.DownloadTable.CREATE_TIME +
                    ") values(?,?,?,?,?,?)";
            db.execSQL(sql, new Object[]{downloadInfo.getUrl(), downloadInfo.getFilePath(),
                    0, downloadInfo.getContentLength(), downloadInfo.getFinished(), downloadInfo.createTime});
        }
        cursor.close();
    }

    public synchronized long queryLocalLength(TransferInfo info) {
        long length = 0;
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "select * from " + Provider.DownloadTable.TABLE_NAME + " where " + Provider.DownloadTable.URL + "=? and "
                + Provider.DownloadTable.PATH + " =?";
        Cursor cursor = db.rawQuery(sql, new String[]{info.getUrl(), info.getFilePath()});
        while (cursor.moveToNext()) {
//            info.threadNum = cursor.getInt(2);
            info.setContentLength(cursor.getLong(3));
            info.setFinished(cursor.getShort(4));
            info.createTime = cursor.getLong(5);
            length = info.getContentLength();
            break;
        }
        cursor.close();
        return length;
    }

    public synchronized List<TransferInfo> getDownloadList() {
        List<TransferInfo> tasks = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "select * from " + Provider.DownloadTable.TABLE_NAME + " order by " + Provider.DownloadTable.CREATE_TIME;
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            TransferInfo info = new TransferInfo(cursor.getString(0), cursor.getString(1));
//            info.threadNum = cursor.getInt(2);
            info.setContentLength(cursor.getLong(3));
            info.setFinished(cursor.getShort(4));
            info.createTime = cursor.getLong(5);
            info.calculateDownloadProgress();
            tasks.add(info);
        }
        cursor.close();
        return tasks;
    }

    public synchronized void deleteInfo(String url, String filePath) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int result = db.delete(Provider.DownloadTable.TABLE_NAME, Provider.DownloadTable.URL + "=? and "
                + Provider.DownloadTable.PATH + " =?", new String[]{url, filePath});
        Log.d("tag", "deleteInfo PATH=" + filePath + ";result=" + result);
    }

    public synchronized void close() {
        helper.close();
    }
}
