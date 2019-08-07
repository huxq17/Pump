package com.huxq17.download.manager;

import android.content.Context;

import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.DownloadRequest;

import java.io.File;
import java.util.List;

public interface IDownloadManager {
    void start(Context context);

    void submit(DownloadRequest downloadRequest);

    void delete(DownloadInfo downloadInfo);

    void deleteById(String id);

    void deleteByTag(String tag);

    void pause(DownloadInfo downloadInfo);

    void stop(DownloadInfo downloadInfo);

    void resume(DownloadInfo downloadInfo);

    List<DownloadDetailsInfo> getDownloadingList();

    List<DownloadDetailsInfo> getDownloadedList();

    List<DownloadDetailsInfo> getDownloadListByTag(String tag);

    List<DownloadDetailsInfo> getAllDownloadList();

    DownloadDetailsInfo getDownloadInfoById(String id);

    boolean hasDownloadSucceed(String id);

    File getFileIfSucceed(String id);

    void setDownloadConfig(DownloadConfig downloadConfig);

    void shutdown();

    boolean isShutdown();

    Context getContext();
}
