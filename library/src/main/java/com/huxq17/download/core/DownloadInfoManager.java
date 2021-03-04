package com.huxq17.download.core;

import android.database.Cursor;
import android.net.Uri;

import com.huxq17.download.DownloadProvider;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadInfoManager {
    private ConcurrentHashMap<String, DownloadDetailsInfo> downloadInfoMap;

    private DownloadInfoManager() {
        downloadInfoMap = new ConcurrentHashMap<>();
    }

    private static DownloadInfoManager instance = new DownloadInfoManager();

    public static DownloadInfoManager getInstance() {
        return instance;
    }

    public DownloadDetailsInfo get(String id) {
        return downloadInfoMap.get(id);
    }

    public Collection<DownloadDetailsInfo> getAll() {
        return downloadInfoMap.values();
    }

    public DownloadDetailsInfo remove(String id) {
        return downloadInfoMap.remove(id);
    }

    public void clear() {
        downloadInfoMap.clear();
    }

    public DownloadDetailsInfo createDownloadInfo(String url, String filePath, String tag, String id, long createTime, Uri schemaUri) {
        return createDownloadInfo(url, filePath, tag, id, createTime, schemaUri, true);
    }

    public DownloadDetailsInfo createDownloadInfo(String url, String filePath, String tag, String id, long createTime, Uri schemaUri, boolean addInMap) {
        if (url == null || url.length() == 0) {
            throw new IllegalArgumentException("url==null or url.length()==0");
        }
        if (id == null || id.length() == 0) {
            id = url;
        }
        DownloadDetailsInfo downloadInfo = downloadInfoMap.get(id);
        if (downloadInfo == null) {
            downloadInfo = new DownloadDetailsInfo(url, filePath, tag, id, createTime, schemaUri);
            if (addInMap) {
                downloadInfoMap.put(id, downloadInfo);
            }
        }
        return downloadInfo;
    }

    public DownloadDetailsInfo createInfoByCursor(Cursor cursor) {
        String id = cursor.getString(7);
        DownloadDetailsInfo info = downloadInfoMap.get(id);
        if (info == null) {
            String uriString = cursor.getString(cursor.getColumnIndex(DownloadProvider.DownloadTable.SCHEMA_URI));

            info = new DownloadDetailsInfo(cursor.getString(cursor.getColumnIndex(DownloadProvider.DownloadTable.URL)),
                    cursor.getString(cursor.getColumnIndex(DownloadProvider.DownloadTable.PATH)),
                    cursor.getString(cursor.getColumnIndex(DownloadProvider.DownloadTable.TAG)),
                    id,
                    cursor.getLong(cursor.getColumnIndex(DownloadProvider.DownloadTable.CREATE_TIME)),
                    uriString == null || uriString.isEmpty() ? null : Uri.parse(uriString));
            info.setThreadNum(cursor.getInt(cursor.getColumnIndex(DownloadProvider.DownloadTable.THREAD_NUM)));
            info.setContentLength(cursor.getLong(cursor.getColumnIndex(DownloadProvider.DownloadTable.FILE_LENGTH)));
            info.setFinished(cursor.getShort(cursor.getColumnIndex(DownloadProvider.DownloadTable.FINISHED)));
            info.calculateDownloadProgress();
            downloadInfoMap.put(id, info);
        }
        return info;
    }

}
