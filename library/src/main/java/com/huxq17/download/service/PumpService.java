package com.huxq17.download.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.TaskManager;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

import java.util.concurrent.Semaphore;

public class PumpService extends Service implements Task {
    private IDownloadManager downloadManager;
    private boolean isRunning;
    private Semaphore semaphore;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        downloadManager = ServiceAgency.getService(IDownloadManager.class);
        //TODO 限制同时进行下载的任务数量
        semaphore = new Semaphore(3, true);
        TaskManager.execute(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
    }

    @Override
    public void run() {
        while (isRunning) {
            try {
                semaphore.acquire();
                DownloadTask downloadTask = downloadManager.take();
                downloadTask.setSemaphore(semaphore);
                TaskManager.execute(downloadTask);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
