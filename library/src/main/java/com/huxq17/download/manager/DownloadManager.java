package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.service.PumpService;
import com.huxq17.download.task.DownloadTask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

@ServiceAgent
public class DownloadManager implements IDownloadManager, DownLoadLifeCycleObserver {
    private Context context;
    LinkedBlockingQueue<DownloadTask> readyTaskQueue;
    LinkedBlockingQueue<DownloadTask> runningTaskQueue;
    private Semaphore semaphore;
    private int maxRunningTaskNumber = 2;
    private boolean isServiceRunning = false;

    public void submit(DownloadInfo downloadInfo) {
        if (semaphore == null) {
            semaphore = new Semaphore(maxRunningTaskNumber);
        }
        DownloadTask downloadTask = new DownloadTask(downloadInfo, this);
        readyTaskQueue.offer(downloadTask);
        if (!isServiceRunning) {
            context.startService(new Intent(context, PumpService.class));
            isServiceRunning = true;
        }
    }

    @Override
    public void shutdown() {
        context.stopService(new Intent(context, PumpService.class));
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
