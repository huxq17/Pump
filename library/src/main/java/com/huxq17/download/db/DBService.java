package com.huxq17.download.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfoManager;

import java.util.ArrayList;
import java.util.List;

public class DBService {
    private DBOpenHelper helper;
    private static DBService instance;
    private DownloadInfoManager downloadInfoManager;

    public static void init(Context context) {
        instance = new DBService(context);
    }

    private DBService(Context context) {
        helper = new DBOpenHelper(context);
        downloadInfoManager = DownloadInfoManager.getInstance();
    }

    public static DBService getInstance() {
        return instance;
    }

    public SQLiteDatabase getWritableDatabase() {
        return helper.getWritableDatabase();
    }

    public synchronized void updateCache(DownloadProvider.CacheBean cacheBean) {
        if (TextUtils.isEmpty(cacheBean.lastModified) && TextUtils.isEmpty(cacheBean.eTag)) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DownloadProvider.CacheTable.URL, cacheBean.url);
        contentValues.put(DownloadProvider.CacheTable.LAST_MODIFIED, cacheBean.lastModified);
        contentValues.put(DownloadProvider.CacheTable.ETAG, cacheBean.eTag);
        db.replace(DownloadProvider.CacheTable.TABLE_NAME, null, contentValues);
    }

    public synchronized DownloadProvider.CacheBean queryCache(String url) {
        SQLiteDatabase db = getWritableDatabase();
        String querySql = "select * from " + DownloadProvider.CacheTable.TABLE_NAME + " where " + DownloadProvider.CacheTable.URL + "=?";
        Cursor cursor = db.rawQuery(querySql, new String[]{url});

        if (cursor.moveToNext()) {
            DownloadProvider.CacheBean cacheBean = new DownloadProvider.CacheBean(url, cursor.getString(2),
                    cursor.getString(1));
            cursor.close();
            return cacheBean;
        }
        cursor.close();
        return null;
    }

    public synchronized void updateInfo(DownloadDetailsInfo downloadInfo) {
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {downloadInfo.getId()};
        Cursor cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, new String[]{DownloadProvider.DownloadTable.URL}, DownloadProvider.DownloadTable.ID + "=?", args, null, null, null, null);
        if (cursor.getCount() == 1) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DownloadProvider.DownloadTable.URL, downloadInfo.getUrl());
            contentValues.put(DownloadProvider.DownloadTable.PATH, downloadInfo.getFilePath());
            contentValues.put(DownloadProvider.DownloadTable.THREAD_NUM, 0);
            contentValues.put(DownloadProvider.DownloadTable.FILE_LENGTH, downloadInfo.getContentLength());
            contentValues.put(DownloadProvider.DownloadTable.FINISHED, downloadInfo.getFinished());
            contentValues.put(DownloadProvider.DownloadTable.TAG, downloadInfo.getTag());
            contentValues.put(DownloadProvider.DownloadTable.ID, downloadInfo.getId());
            db.update(DownloadProvider.DownloadTable.TABLE_NAME, contentValues,
                    DownloadProvider.DownloadTable.ID + "=?", new String[]{downloadInfo.getId()});
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(DownloadProvider.DownloadTable.URL, downloadInfo.getUrl());
            contentValues.put(DownloadProvider.DownloadTable.PATH, downloadInfo.getFilePath());
            contentValues.put(DownloadProvider.DownloadTable.THREAD_NUM, 0);
            contentValues.put(DownloadProvider.DownloadTable.FILE_LENGTH, downloadInfo.getContentLength());
            contentValues.put(DownloadProvider.DownloadTable.FINISHED, downloadInfo.getFinished());
            contentValues.put(DownloadProvider.DownloadTable.CREATE_TIME, downloadInfo.getCreateTime());
            contentValues.put(DownloadProvider.DownloadTable.TAG, downloadInfo.getTag());
            contentValues.put(DownloadProvider.DownloadTable.ID, downloadInfo.getId());
            db.insert(DownloadProvider.DownloadTable.TABLE_NAME, null, contentValues);
        }
        cursor.close();
    }

    public synchronized long queryLocalLength(DownloadDetailsInfo info) {
        long length = 0;
        SQLiteDatabase db = getWritableDatabase();
        String[] args = {info.getId()};
        Cursor cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, new String[]{DownloadProvider.DownloadTable.FILE_LENGTH}, DownloadProvider.DownloadTable.ID + "=?", args, null, null, null, null);
        while (cursor.moveToNext()) {
//            info.threadNum = cursor.getInt(2);
            length = cursor.getLong(0);
            break;
        }
        cursor.close();
        return length;
    }

    public synchronized List<DownloadDetailsInfo> getDownloadList() {
        return getDownloadListByTag(null);
    }

    public synchronized List<DownloadDetailsInfo> getDownloadListByTag(String tag) {
        List<DownloadDetailsInfo> tasks = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor;
        if (tag == null) {
            cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, null, null, null, null, null, DownloadProvider.DownloadTable.CREATE_TIME, null);
        } else {
            cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, null, DownloadProvider.DownloadTable.TAG + " = ?", new String[]{tag}, null, null, DownloadProvider.DownloadTable.CREATE_TIME, null);
        }
        while (cursor.moveToNext()) {
            DownloadDetailsInfo info = downloadInfoManager.createInfoByCursor(cursor);
            tasks.add(info);
        }
        cursor.close();
        return tasks;
    }

    public synchronized DownloadDetailsInfo getDownloadInfo(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is empty.");
        }
        DownloadDetailsInfo info = null;
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, null,
                DownloadProvider.DownloadTable.ID + "=?", new String[]{id}, null, null, null, null);
        while (cursor.moveToNext()) {
            info = downloadInfoManager.createInfoByCursor(cursor);
        }
        cursor.close();
        return info;
    }

    public synchronized void deleteInfo(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(DownloadProvider.DownloadTable.TABLE_NAME, DownloadProvider.DownloadTable.ID + "=?", new String[]{id});
        db.delete(DownloadProvider.CacheTable.TABLE_NAME, DownloadProvider.CacheTable.URL + "=?", new String[]{id});
    }

    public synchronized void close() {
        helper.close();
    }
}
