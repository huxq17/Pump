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
import java.util.concurrent.atomic.AtomicInteger;

public class DBService {
    private DBOpenHelper helper;
    private static DBService instance;
    private DownloadInfoManager downloadInfoManager;
    private AtomicInteger mOpenCounter = new AtomicInteger();

    public static void init(Context context) {
        instance = new DBService(context);
    }

    private DBService(Context context) {
        helper = new DBOpenHelper(context);
        downloadInfoManager = DownloadInfoManager.getInstance();
    }

    public static synchronized DBService getInstance() {
        return instance;
    }

    public void updateCache(DownloadProvider.CacheBean cacheBean) {
        if (TextUtils.isEmpty(cacheBean.lastModified) && TextUtils.isEmpty(cacheBean.eTag)) {
            return;
        }
        SQLiteDatabase db = getDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DownloadProvider.CacheTable.URL, cacheBean.url);
        contentValues.put(DownloadProvider.CacheTable.LAST_MODIFIED, cacheBean.lastModified);
        contentValues.put(DownloadProvider.CacheTable.ETAG, cacheBean.eTag);
        db.replace(DownloadProvider.CacheTable.TABLE_NAME, null, contentValues);
        closeDatabase();
    }

    public DownloadProvider.CacheBean queryCache(String url) {
        SQLiteDatabase db = getDatabase();
        String querySql = "select * from " + DownloadProvider.CacheTable.TABLE_NAME + " where " + DownloadProvider.CacheTable.URL + "=?";
        Cursor cursor = db.rawQuery(querySql, new String[]{url});
        DownloadProvider.CacheBean cacheBean = null;
        if (cursor.moveToNext()) {
            cacheBean = new DownloadProvider.CacheBean(url, cursor.getString(2),
                    cursor.getString(1));
        }
        cursor.close();
        closeDatabase();
        return cacheBean;
    }

    public void updateInfo(DownloadDetailsInfo downloadInfo) {
        if (downloadInfo.isDeleted()) {
            return;
        }
        SQLiteDatabase db = getDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DownloadProvider.DownloadTable.URL, downloadInfo.getUrl());
        contentValues.put(DownloadProvider.DownloadTable.PATH, downloadInfo.getFilePath());
        contentValues.put(DownloadProvider.DownloadTable.THREAD_NUM, downloadInfo.getThreadNum());
        contentValues.put(DownloadProvider.DownloadTable.FILE_LENGTH, downloadInfo.getContentLength());
        contentValues.put(DownloadProvider.DownloadTable.FINISHED, downloadInfo.getFinished());
        contentValues.put(DownloadProvider.DownloadTable.TAG, downloadInfo.getTag());
        contentValues.put(DownloadProvider.DownloadTable.ID, downloadInfo.getId());
        contentValues.put(DownloadProvider.DownloadTable.CREATE_TIME, downloadInfo.getCreateTime());
        contentValues.put(DownloadProvider.DownloadTable.SCHEMA_URI, downloadInfo.getSchemaUri() == null ? null : downloadInfo.getSchemaUri().toString());
        db.replace(DownloadProvider.DownloadTable.TABLE_NAME, null, contentValues);
        closeDatabase();
    }

    public List<DownloadDetailsInfo> getDownloadList() {
        return getDownloadListByTag(null);
    }

    public List<DownloadDetailsInfo> getDownloadListByTag(String tag) {
        List<DownloadDetailsInfo> tasks = new ArrayList<>();
        SQLiteDatabase db = getDatabase();
        Cursor cursor;
        if (tag == null) {
            cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, null,
                    null, null, null, null,
                    DownloadProvider.DownloadTable.CREATE_TIME + " DESC", null);
        } else {
            cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, null,
                    DownloadProvider.DownloadTable.TAG + " = ?", new String[]{tag},
                    null, null, DownloadProvider.DownloadTable.CREATE_TIME + " DESC", null);
        }
        while (cursor.moveToNext()) {
            DownloadDetailsInfo info = downloadInfoManager.createInfoByCursor(cursor);
            tasks.add(info);
        }
        cursor.close();
        closeDatabase();
        return tasks;
    }

    public DownloadDetailsInfo getDownloadInfo(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is empty.");
        }
        DownloadDetailsInfo info = null;
        SQLiteDatabase db = getDatabase();
        Cursor cursor = db.query(DownloadProvider.DownloadTable.TABLE_NAME, null,
                DownloadProvider.DownloadTable.ID + "=?", new String[]{id}, null, null, null, null);
        if (cursor.moveToNext()) {
            info = downloadInfoManager.createInfoByCursor(cursor);
        }
        cursor.close();
        closeDatabase();
        return info;
    }

    public void deleteInfo(String id) {
        SQLiteDatabase db = getDatabase();
        db.delete(DownloadProvider.DownloadTable.TABLE_NAME, DownloadProvider.DownloadTable.ID + "=?", new String[]{id});
        db.delete(DownloadProvider.CacheTable.TABLE_NAME, DownloadProvider.CacheTable.URL + "=?", new String[]{id});
        closeDatabase();
    }

    private synchronized SQLiteDatabase getDatabase() {
        mOpenCounter.incrementAndGet();
        return helper.getWritableDatabase();
    }

    private synchronized void closeDatabase() {
        if (mOpenCounter.decrementAndGet() == 0) {
            helper.close();
        }
    }
}
