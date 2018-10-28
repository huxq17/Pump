package com.huxq17.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.huxq17.download.DownloadInfo;
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

    public void updateInfo(DownloadInfo downloadInfo) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String sql = "replace into " + Provider.DownloadInfo.TABLE_NAME +
                "(" + Provider.DownloadInfo.URL + ", "
                + Provider.DownloadInfo.PATH + ","
                + Provider.DownloadInfo.THREAD_NUM + ","
                + Provider.DownloadInfo.FILE_LENGTH + ","
                + Provider.DownloadInfo.FINISHED + ") values(?,?,?,?,?)";
        db.execSQL(sql, new Object[]{downloadInfo.url, downloadInfo.filePath,
                downloadInfo.threadNum, downloadInfo.contentLength, downloadInfo.finished});
    }

    public long queryLocalLength(DownloadInfo info) {
        long length = 0;
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "select * from " + Provider.DownloadInfo.TABLE_NAME + " where " + Provider.DownloadInfo.URL + "=? and "
                + Provider.DownloadInfo.PATH + " =?";
        Cursor cursor = db.rawQuery(sql, new String[]{info.url, info.filePath});
        while (cursor.moveToNext()) {
            info.filePath = cursor.getString(1);
            info.threadNum = cursor.getInt(2);
            info.contentLength = cursor.getLong(3);
            info.finished = cursor.getShort(4);
            length = info.contentLength;
            break;
        }
        cursor.close();
        return length;
    }

    public List<DownloadInfo> getDownloadList(boolean isAll,boolean isFinished) {
        List<DownloadInfo> tasks = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "select * from " + Provider.DownloadInfo.TABLE_NAME;
        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            int finished = cursor.getShort(4);
            if (isAll||isFinished && finished == 1 || !isFinished && finished == 0) {
                DownloadInfo info = new DownloadInfo();
                info.url = cursor.getString(0);
                info.filePath = cursor.getString(1);
                info.threadNum = cursor.getInt(2);
                info.contentLength = cursor.getLong(3);
                info.finished = finished;
                tasks.add(info);
            }
        }
        return tasks;
    }

    public void clearByUrl(String url) {
        deleteInfoByUrl(url);
    }


    public void deleteInfoByUrl(String url) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int result = db.delete(Provider.DownloadInfo.TABLE_NAME, Provider.DownloadInfo.URL + "=?", new String[]{url});
        Log.d("tag", "deleteInfo url=" + url + ";result=" + result);
    }

    public void close() {
        helper.close();
    }
}
