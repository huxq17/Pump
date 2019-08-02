package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadTaskExecutor implements Task {
    private AtomicBoolean isRunning = new AtomicBoolean();
    private DownloadService downloadService;

    public DownloadTaskExecutor(DownloadService downloadService) {
        this.downloadService = downloadService;
        TaskManager.execute(this);
    }

    public void start() {
        if (!isRunning.get()) {
            isRunning.set(true);
            TaskManager.execute(this);
        }
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            DownloadTask downloadTask = downloadService.getDownloadTask();
            if (downloadTask == null) {
                isRunning .set(false);
                break;
            }
            LogUtil.d("start run " + downloadTask.getName());
            TaskManager.execute(downloadTask);
        }
        LogUtil.d("DownloadTaskExecutor end.");
    }

    @Override
    public void cancel() {
    }

}
