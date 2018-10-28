package com.huxq17.download.manager;

import android.content.Context;

import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.task.DownloadTask;

import java.util.List;

public interface IDownloadManager {
    void start(Context context);

    DownloadTask take() throws InterruptedException;

    void submit(DownloadInfo downloadInfo);

    List<DownloadInfo> getDownloadingList();

    List<DownloadInfo> getDownloadedList();

    List<DownloadInfo> getAllDownloadList();

    void setDownloadConfig(DownloadConfig downloadConfig);

    void shutdown();
}
