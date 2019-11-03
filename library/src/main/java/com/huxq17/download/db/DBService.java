package com.huxq17.download.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.huxq17.download.DownloadDetailsInfo;
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

    public SQLiteDatabase getWritableDatabase() {
        return helper.getWritableDatabase();
    }

    public synchronized void updateCache(Provider.CacheBean cacheBean) {
        if (TextUtils.isEmpty(cacheBean.lastModified) && TextUtils.isEmpty(cacheBean.eTag)) {
            return;
        }
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(Provider.CacheTable.URL, cacheBean.url);
        contentValues.put(Provider.CacheTable.LAST_MODIFIED, cacheBean.lastModified);
        contentValues.put(Provider.CacheTable.ETAG, cacheBean.eTag);
        db.replace(Provider.CacheTable.TABLE_NAME, null, contentValues);
    }

    public synchronized Provider.CacheBean queryCache(String url) {
        SQLiteDatabase db = helper.getReadableDatabase();
        String querySql = "select * from " + Provider.CacheTable.TABLE_NAME + " where " + Provider.CacheTable.URL + "=?";
        Cursor cursor = db.rawQuery(querySql, new String[]{url});

        if (cursor.moveToNext()) {
            Provider.CacheBean cacheBean = new Provider.CacheBean(url, cursor.getString(2),
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
        Cursor cursor = db.query(Provider.DownloadTable.TABLE_NAME, new String[]{Provider.DownloadTable.URL}, Provider.DownloadTable.ID + "=?", args, null, null, null, null);
        if (cursor.getCount() == 1) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Provider.DownloadTable.URL, downloadInfo.getUrl());
            contentValues.put(Provider.DownloadTable.PATH, downloadInfo.getFilePath());
            contentValues.put(Provider.DownloadTable.THREAD_NUM, 0);
            contentValues.put(Provider.DownloadTable.FILE_LENGTH, downloadInfo.getContentLength());
            contentValues.put(Provider.DownloadTable.FINISHED, downloadInfo.getFinished());
            contentValues.put(Provider.DownloadTable.TAG, downloadInfo.getTag());
            contentValues.put(Provider.DownloadTable.ID, downloadInfo.getId());
            db.update(Provider.DownloadTable.TABLE_NAME, contentValues,
                    Provider.DownloadTable.ID + "=?", new String[]{downloadInfo.getId()});
        } else {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Provider.DownloadTable.URL, downloadInfo.getUrl());
            contentValues.put(Provider.DownloadTable.PATH, downloadInfo.getFilePath());
            contentValues.put(Provider.DownloadTable.THREAD_NUM, 0);
            contentValues.put(Provider.DownloadTable.FILE_LENGTH, downloadInfo.getContentLength());
            contentValues.put(Provider.DownloadTable.FINISHED, downloadInfo.getFinished());
            contentValues.put(Provider.DownloadTable.CREATE_TIME, downloadInfo.getCreateTime());
            contentValues.put(Provider.DownloadTable.TAG, downloadInfo.getTag());
            contentValues.put(Provider.DownloadTable.ID, downloadInfo.getId());
            db.insert(Provider.DownloadTable.TABLE_NAME, null, contentValues);
        }
        cursor.close();
    }

    public synchronized long queryLocalLength(DownloadDetailsInfo info) {
        long length = 0;
        SQLiteDatabase db = helper.getReadableDatabase();
        String[] args = {info.getId()};
        Cursor cursor = db.query(Provider.DownloadTable.TABLE_NAME, new String[]{Provider.DownloadTable.FILE_LENGTH}, Provider.DownloadTable.ID + "=?", args, null, null, null, null);
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
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor;
        if (tag == null) {
            cursor = db.query(Provider.DownloadTable.TABLE_NAME, null, null, null, null, null, Provider.DownloadTable.CREATE_TIME, null);
        } else {
            cursor = db.query(Provider.DownloadTable.TABLE_NAME, null, Provider.DownloadTable.TAG + " = ?", new String[]{tag}, null, null, Provider.DownloadTable.CREATE_TIME, null);
        }
        while (cursor.moveToNext()) {
            DownloadDetailsInfo info = new DownloadDetailsInfo(cursor.getString(0), cursor.getString(1), cursor.getString(6), cursor.getString(7));
//            info.threadNum = cursor.getInt(2);
            info.setContentLength(cursor.getLong(3));
            info.setFinished(cursor.getShort(4));
            info.setCreateTime(cursor.getLong(5));
            info.setId(cursor.getString(7));
            info.calculateDownloadProgress();
//            LogUtil.d("table " + info);
            tasks.add(info);
        }
        cursor.close();
        return tasks;
    }

    public synchronized DownloadDetailsInfo getDownloadInfo(String id) {
        if (TextUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id is empty.");
        }
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(Provider.DownloadTable.TABLE_NAME, null,
                Provider.DownloadTable.ID + "=?", new String[]{id}, null, null, null, null);
        DownloadDetailsInfo info = null;
        while (cursor.moveToNext()) {
            info = new DownloadDetailsInfo(cursor.getString(0), cursor.getString(1), cursor.getString(6), cursor.getString(7));
            info.setContentLength(cursor.getLong(3));
            info.setFinished(cursor.getShort(4));
            info.setCreateTime(cursor.getLong(5));
            info.calculateDownloadProgress();
        }
        cursor.close();
        return info;
    }

    public synchronized void deleteInfo(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(Provider.DownloadTable.TABLE_NAME, Provider.DownloadTable.ID + "=?", new String[]{id});
        db.delete(Provider.CacheTable.TABLE_NAME, Provider.CacheTable.URL + "=?", new String[]{id});
    }

    public synchronized void close() {
        helper.close();
    }
}
