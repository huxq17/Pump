package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.service.PumpService;
import com.huxq17.download.task.DownloadTask;

import java.util.concurrent.LinkedBlockingQueue;

@ServiceAgent
public class DownloadManager implements IDownloadManager {
    private Context context;
    LinkedBlockingQueue<DownloadTask> downloadQueue;
    private boolean isServiceRunning = false;

    public void submit(DownloadInfo downloadInfo) {
        downloadQueue.offer(new DownloadTask(downloadInfo));
        if (!isServiceRunning) {
            context.startService(new Intent(context, PumpService.class));
        }
    }

    @Override
    public void shutdown() {
        context.stopService(new Intent(context, PumpService.class));
    }

    public DownloadTask take() throws InterruptedException {
        return downloadQueue.take();
    }

    @Override
    public void start(Context context) {
        this.context = context;
        isServiceRunning = false;
        downloadQueue = new LinkedBlockingQueue<>();
    }
}
