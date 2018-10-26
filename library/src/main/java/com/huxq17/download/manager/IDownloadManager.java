package com.huxq17.download.manager;

import android.content.Context;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.task.DownloadTask;

public interface IDownloadManager {
    void start(Context context);

    DownloadTask take() throws InterruptedException;

    void submit(DownloadInfo downloadInfo);

    void shutdown();
}
