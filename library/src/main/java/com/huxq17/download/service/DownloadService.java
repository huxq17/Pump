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

public class DownloadService extends Service implements Task {
    private IDownloadManager downloadManager;
    private boolean isRunning;

    @Override
    public void onCreate() {
        super.onCreate();
        isRunning = true;
        downloadManager = ServiceAgency.getService(IDownloadManager.class);
        TaskManager.execute(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
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
                DownloadTask downloadTask = downloadManager.acquireTask();
                if (downloadTask != null) {
                    TaskManager.execute(downloadTask);
                } else {
                    isRunning = false;
                    stopSelf();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
