package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;


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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class DownloadManager implements IDownloadManager, DownLoadLifeCycleObserver {
    private Context context;
    LinkedBlockingQueue<DownloadTask> readyTaskQueue;
    LinkedBlockingQueue<DownloadTask> runningTaskQueue;
    HashMap<String,DownloadTask> taskMap;
    private Semaphore semaphore;
    private boolean isServiceRunning = false;
    private long maxCreateTime;

    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;
    private boolean isShutdown = true;

    private DownloadManager() {
        taskMap = new HashMap<>();
    }

    private DownloadDetailsInfo getDownloadInfo(String url, String filePath, String tag) {
        DownloadDetailsInfo downloadInfo = DBService.getInstance().getDownloadInfo(url);
        if (downloadInfo != null) {
            return downloadInfo;
        }
        //create a new instance if not found.
        downloadInfo = new DownloadDetailsInfo(url, filePath, tag);
        maxCreateTime++;
        downloadInfo.createTime = maxCreateTime;
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    public synchronized void submit(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        String filePath = downloadRequest.getFilePath();
        String tag = downloadRequest.getTag();
        DownloadDetailsInfo downloadInfo = getDownloadInfo(url, filePath, tag);
        downloadRequest.setDownloadInfo(downloadInfo);
        if (taskMap.get(url)!=null) {
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
        taskMap.put(downloadRequest.getUrl(),downloadTask);
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
            DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
            DownloadTask downloadTask = transferInfo.getDownloadTask();
            if(downloadTask==null){
                downloadTask = taskMap.get(downloadInfo.getUrl());
            }
            if (downloadTask != null) {
                readyTaskQueue.remove(downloadTask);
                downloadTask.delete();
            }
            transferInfo.getDownloadFile().delete();
            Util.deleteDir(transferInfo.getTempDir());
            DBService.getInstance().deleteInfo(downloadInfo.getUrl(), downloadInfo.getFilePath());
        }
    }
    public synchronized void delete(String tag) {
        if (tag == null) return;
        List<DownloadDetailsInfo> tasks = DBService.getInstance().getDownloadListByTag(tag);
        for (DownloadDetailsInfo info : tasks) {
            delete(info);
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
        } else {
            DownloadRequest.newRequest(transferInfo.getUrl(), transferInfo.getFilePath()).submit();
        }
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadingList() {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        List<DownloadDetailsInfo> list = DBService.getInstance().getDownloadList();
        for (DownloadDetailsInfo info : list) {
            if (!info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadedList() {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        List<DownloadDetailsInfo> list = DBService.getInstance().getDownloadList();
        for (DownloadDetailsInfo info : list) {
            if (info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getAllDownloadList() {
        return new ArrayList<>(DBService.getInstance().getDownloadList());
    }

    @Override
    public boolean hasCached(String url) {
        DownloadDetailsInfo info = DBService.getInstance().getDownloadInfo(url);
        if (info != null && info.isFinished()) {
            return true;
        }
        return false;
    }

    @Override
    public File getFileFromCache(String url) {
        if (hasCached(url)) {
            DownloadDetailsInfo info = DBService.getInstance().getDownloadInfo(url);
            return info.getDownloadFile();
        }
        return null;
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
        List<DownloadDetailsInfo> downloadList = DBService.getInstance().getDownloadList();
        for (DownloadDetailsInfo transferInfo : downloadList) {
            stop(transferInfo);
        }
        DownloadInfoSnapshot.release();
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    @Override
    public Context getContext() {
        return context;
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
        taskMap.remove(downloadInfo.getUrl());
        semaphore.release();
    }
}
