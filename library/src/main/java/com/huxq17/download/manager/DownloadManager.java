package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.service.DownloadService;
import com.huxq17.download.task.DownloadTask;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

@ServiceAgent
public class DownloadManager implements IDownloadManager, DownLoadLifeCycleObserver {
    private Context context;
    LinkedBlockingQueue<DownloadTask> readyTaskQueue;
    LinkedBlockingQueue<DownloadTask> runningTaskQueue;
    private Semaphore semaphore;
    private int maxRunningTaskNumber;
    private boolean isServiceRunning = false;
    private DownloadConfig downloadConfig;

    public void submit(DownloadInfo downloadInfo) {
        if (downloadConfig == null) {
            downloadConfig = new DownloadConfig();
        }
        downloadInfo.threadNum = downloadConfig.downloadThreadNumber;
        downloadInfo.forceReDownload = downloadConfig.forceReDownload;
        maxRunningTaskNumber = downloadConfig.maxRunningTaskNumber;
        if (semaphore == null) {
            semaphore = new Semaphore(maxRunningTaskNumber);
        }
        DownloadTask downloadTask = new DownloadTask(downloadInfo, this);
        readyTaskQueue.offer(downloadTask);
        if (!isServiceRunning) {
            context.startService(new Intent(context, DownloadService.class));
            isServiceRunning = true;
        }
    }

    @Override
    public List<DownloadInfo> getDownloadingList() {
        return DBService.getInstance().getDownloadList(false, false);
    }

    @Override
    public List<DownloadInfo> getDownloadedList() {
        return DBService.getInstance().getDownloadList(false, true);
    }

    @Override
    public List<DownloadInfo> getAllDownloadList() {
        return DBService.getInstance().getDownloadList(true, true);
    }

    @Override
    public void setDownloadConfig(DownloadConfig downloadConfig) {
        this.downloadConfig = downloadConfig;
    }

    @Override
    public void shutdown() {
        context.stopService(new Intent(context, DownloadService.class));
    }

    public DownloadTask take() throws InterruptedException {
        semaphore.acquire();
        return readyTaskQueue.take();
    }

    @Override
    public void start(Context context) {
        this.context = context;
        isServiceRunning = false;
        readyTaskQueue = new LinkedBlockingQueue<>();
        runningTaskQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void onDownloadStart(DownloadTask downloadTask) {
        runningTaskQueue.add(downloadTask);
    }

    @Override
    public void onDownloadEnd(DownloadTask downloadTask) {
        runningTaskQueue.remove(downloadTask);
        semaphore.release();
    }
}
