package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.service.DownloadService;
import com.huxq17.download.task.DownloadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

@ServiceAgent
public class DownloadManager implements IDownloadManager, DownLoadLifeCycleObserver {
    private Context context;
    LinkedBlockingQueue<DownloadTask> readyTaskQueue;
    LinkedBlockingQueue<DownloadTask> runningTaskQueue;
    private Semaphore semaphore;
    private boolean isServiceRunning = false;
    private HashMap<String, DownloadDetailsInfo> allDownloadInfo = new LinkedHashMap<>();
    private long maxCreateTime;

    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;
    private boolean isShutdown = true;

    private DownloadManager() {
        List<DownloadDetailsInfo> allDownloadInfo = DBService.getInstance().getDownloadList();
        for (DownloadDetailsInfo transferInfo : allDownloadInfo) {
            if (transferInfo.createTime > maxCreateTime) {
                maxCreateTime = transferInfo.createTime;
            }
            this.allDownloadInfo.put(transferInfo.getUrl(), transferInfo);
        }
    }

    private DownloadDetailsInfo getDownloadInfo(String url, String filePath) {
        DownloadDetailsInfo downloadInfo = allDownloadInfo.get(url);
        if (downloadInfo != null) {
            return downloadInfo;
        }
        //create a new instance if not found.
        downloadInfo = new DownloadDetailsInfo(url, filePath);
        maxCreateTime++;
        downloadInfo.createTime = maxCreateTime;
        allDownloadInfo.put(url, downloadInfo);
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    public synchronized void submit(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        String filePath = downloadRequest.getFilePath();
        DownloadDetailsInfo downloadInfo = getDownloadInfo(url, filePath);
        downloadRequest.setDownloadInfo(downloadInfo);
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        if (downloadTask != null && (readyTaskQueue.contains(downloadTask) || runningTaskQueue.contains(downloadTask))) {
            //The task is running,we need do nothing.
            LogUtil.e("task " + downloadInfo.getName() + " is running,we need do nothing.");
            return;
        }
//        if (downloadInfo.isFinished()) {
//            downloadInfo.setCompletedSize(0);
//        }
//            downloadInfo.calculateDownloadProgress();
//            downloadInfo.setTag(null);
        downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
//            if (downloadInfo.getDownloadFile().exists()) {
//                downloadInfo.getDownloadFile().delete();
//            }
        submitTask(downloadRequest);
    }

    private void submitTask(DownloadRequest downloadRequest) {
        isShutdown = false;
        if (semaphore == null) {
            semaphore = new Semaphore(maxRunningTaskNumber);
        }
        DownloadTask downloadTask = new DownloadTask(downloadRequest, this);
        readyTaskQueue.offer(downloadTask);
        LogUtil.d("task " + downloadRequest.getDownloadInfo().getName() + " is ready" + ",remaining " + semaphore.availablePermits() + " permits.");
        if (!isServiceRunning) {
            context.startService(new Intent(context, DownloadService.class));
            isServiceRunning = true;
        }
    }

    public synchronized void delete(DownloadInfo downloadInfo) {
        if (downloadInfo == null) return;
        synchronized (downloadInfo) {
            String url = downloadInfo.getUrl();
            if (allDownloadInfo.containsKey(url)) {
                allDownloadInfo.remove(url);
                DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
                DownloadTask downloadTask = transferInfo.getDownloadTask();
                if (downloadTask != null) {
                    readyTaskQueue.remove(downloadTask);
                    downloadTask.delete();
                }
                transferInfo.getDownloadFile().delete();
                Util.deleteDir(transferInfo.getTempDir());
                DBService.getInstance().deleteInfo(downloadInfo.getUrl(), downloadInfo.getFilePath());
            }
        }
    }

    @Override
    public void stop(DownloadInfo downloadInfo) {
        DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null) {
            downloadTask.stop();
        }
        DBService.getInstance().close();
    }

    @Override
    public void pause(DownloadInfo downloadInfo) {
        for (DownloadTask task : runningTaskQueue) {
            if (task.getDownloadInfo() == downloadInfo) {
                task.pause();
            }
        }
    }

    @Override
    public synchronized void resume(DownloadInfo downloadInfo) {
        DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null && downloadTask.getRequest() != null) {
            DownloadRequest downloadRequest = downloadTask.getRequest();
            submit(downloadRequest);
        }
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadingList() {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        for (DownloadDetailsInfo info : allDownloadInfo.values()) {
            if (!info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadedList() {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        for (DownloadDetailsInfo info : allDownloadInfo.values()) {
            if (info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getAllDownloadList() {
        return new ArrayList<>(allDownloadInfo.values());
    }

    @Override
    public void setDownloadConfig(DownloadConfig downloadConfig) {
        maxRunningTaskNumber = downloadConfig.getMaxRunningTaskNumber();
    }

    @Override
    public void onServiceDestroy() {
        isServiceRunning = false;
    }

    @Override
    public synchronized void shutdown() {
        isShutdown = true;
        context.stopService(new Intent(context, DownloadService.class));
        for (DownloadDetailsInfo transferInfo : allDownloadInfo.values()) {
            stop(transferInfo);
        }
        DownloadInfoSnapshot.release();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public DownloadTask acquireTask() throws InterruptedException {
        DownloadTask task = readyTaskQueue.take();
        if (semaphore != null) {
            semaphore.acquire();
        }
        return task;
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
        DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
        LogUtil.d("Task " + downloadInfo.getName() + " is stopped.");
        runningTaskQueue.remove(downloadTask);
        semaphore.release();
    }
}
