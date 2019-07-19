package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

public class DownloadService implements Task {
    private IDownloadManager downloadManager;
    private boolean isRunning;

    public DownloadService(IDownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void start() {
        isRunning = true;
        TaskManager.execute(this);
    }

    @Override
    public void run() {
        LogUtil.d("DownloadService start");
        while (isRunning) {
            try {
                DownloadTask downloadTask = downloadManager.acquireTask();
                if (downloadTask == null) {
                    break;
                }
                LogUtil.d("start run task=" + downloadTask.getDownloadInfo().getName());
                TaskManager.execute(downloadTask);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LogUtil.d("DownloadService stopped");
        isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void cancel() {
        Thread.currentThread().interrupt();
    }
}
