package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

public class DownloadTaskExecutor implements Task {
    private boolean isRunning = true;
    private DownloadService downloadService;

    public DownloadTaskExecutor(DownloadService downloadService) {
        this.downloadService = downloadService;
        TaskManager.execute(this);
    }

    @Override
    public void run() {
        while (isRunning) {
            DownloadTask downloadTask = downloadService.getDownloadTask();
            if (downloadTask == null) {
                isRunning = false;
                break;
            }
            LogUtil.d("start run " + downloadTask.getName());
            TaskManager.execute(downloadTask);
        }
        LogUtil.d("DownloadQueueTask end.");
    }

    @Override
    public void cancel() {
        Thread.currentThread().interrupt();
    }

}
