package com.huxq17.download.manager;

import android.content.Context;

import com.huxq17.download.core.DownLoadLifeCycleCallback;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadRequest;

import java.io.File;
import java.util.List;

public interface IDownloadManager  extends DownLoadLifeCycleCallback {
    void start(Context context);

    void submit(DownloadRequest downloadRequest);

    void delete(DownloadInfo downloadInfo);

    void deleteById(String id);

    void deleteByTag(String tag);

    void pause(DownloadInfo downloadInfo);

    void stop(DownloadInfo downloadInfo);

    void resume(DownloadInfo downloadInfo);

    List<DownloadInfo> getDownloadingList();

    List<DownloadInfo> getDownloadedList();

    List<DownloadInfo> getDownloadListByTag(String tag);

    List<DownloadInfo> getAllDownloadList();

    DownloadInfo getDownloadInfoById(String id);

    boolean hasDownloadSucceed(String id);

    boolean isTaskRunning(String id);

    File getFileIfSucceed(String id);

    void shutdown();

    boolean isShutdown();

    Context getContext();
}
