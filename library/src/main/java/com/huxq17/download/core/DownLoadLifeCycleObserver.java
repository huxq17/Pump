package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;

public interface DownLoadLifeCycleObserver {
    void onDownloadStart(DownloadTask downloadTask);

    void onDownloadEnd(DownloadTask downloadTask);
}
