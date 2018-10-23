package com.huxq17.download;


import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.TaskManager;

public class DownloadManager {
    private static class InstanceHolder {
        private static final DownloadManager DOWNLOAD_MANAGER = new DownloadManager();
    }

    public static DownloadManager getInstance() {
        return InstanceHolder.DOWNLOAD_MANAGER;
    }

    public void submit(DownloadInfo downloadInfo) {
        TaskManager.execute(new DownloadTask(downloadInfo));
    }
}
