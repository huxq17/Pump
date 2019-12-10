package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;

public interface DownLoadLifeCycleCallback {
    void onDownloadStart(DownloadTask downloadTask);

    void onDownloadEnd(DownloadTask downloadTask);
}
