package com.huxq17.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.huxq17.download.DownloadBatch;
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

    public List<DownloadBatch> queryLocalBatch(String url) {
        List<DownloadBatch> result = null;
        SQLiteDatabase db = helper.getReadableDatabase();
        String sql = "select " + Provider.DownloadBlock.THREAD_ID + "" +
                "," + Provider.DownloadBlock.COMPLETE_SIZE + " from " + Provider.DownloadBlock.TABLE_NAME
                + " where " + Provider.DownloadBlock.URL + "=?";
        Cursor cursor = db.rawQuery(sql, new String[]{url});
        while (cursor.moveToNext()) {
            DownloadBatch downloadBatch = new DownloadBatch();
            downloadBatch.url = url;
            downloadBatch.threadId = cursor.getInt(0);
            downloadBatch.downloadedSize = cursor.getInt(1);
            if (result == null) {
                result = new ArrayList<>();
            }
            result.add(downloadBatch);
        }
        cursor.close();
        return result;
    }

    public void updateBatch(String url, int threadId, long downloadedSize) {
        SQLiteDatabase db = helper.getWritableDatabase();
        String sql = "replace into " + Provider.DownloadBlock.TABLE_NAME +
                "(" + Provider.DownloadBlock.THREAD_ID + ", " + Provider.DownloadBlock.URL + ","
                + Provider.DownloadBlock.COMPLETE_SIZE + ") values(?,?,?)";
        db.execSQL(sql, new Object[]{threadId, url, downloadedSize});
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
        String sql = "select * from " + Provider.DownloadInfo.TABLE_NAME + " where " + Provider.DownloadInfo.URL + "=?";
        Cursor cursor = db.rawQuery(sql, new String[]{info.url});
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

    public void clearByUrl(String url) {
        deleteBatchByUrl(url);
        deleteInfoByUrl(url);
    }

    public void deleteBatchByUrl(String url) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int result = db.delete(Provider.DownloadBlock.TABLE_NAME,
                Provider.DownloadBlock.URL + "=?", new String[]{url});
        Log.e("tag", "deleteBatch url=" + url + ";result=" + result);
    }

    public void deleteInfoByUrl(String url) {
        SQLiteDatabase db = helper.getWritableDatabase();
        int result = db.delete(Provider.DownloadInfo.TABLE_NAME, Provider.DownloadInfo.URL + "=?", new String[]{url});
        Log.e("tag", "deleteInfo url=" + url + ";result=" + result);
    }

    public void close() {
        helper.close();
    }
}
