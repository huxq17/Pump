package com.huxq17.download.listener;

import com.huxq17.download.task.DownloadTask;

public interface DownLoadLifeCycleObserver {
    void onDownloadStart(DownloadTask downloadTask);

    void onDownloadEnd(DownloadTask downloadTask);
}
