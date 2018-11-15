package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.message.IMessageCenter;
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
    private HashMap<String, TransferInfo> allDownloadInfo = new LinkedHashMap<>();
    private long maxCreateTime;

    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;

    private DownloadManager() {
        List<TransferInfo> allDownloadInfo = DBService.getInstance().getDownloadList();
        for (TransferInfo transferInfo : allDownloadInfo) {
            if (transferInfo.createTime > maxCreateTime) {
                maxCreateTime = transferInfo.createTime;
            }
            this.allDownloadInfo.put(transferInfo.getFilePath(), transferInfo);
        }
    }

    private TransferInfo getDownloadInfo(String url, String filePath) {
        TransferInfo downloadInfo = allDownloadInfo.get(filePath);
        if (downloadInfo != null) {
            String localUrl = downloadInfo.getUrl();
            if (localUrl.equals(url)) {
                return downloadInfo;
            } else {
                delete(downloadInfo);
            }
//            if (!downloadInfo.isUsed()) {
//            } else {
//                try {
//                    TransferInfo transferInfo = downloadInfo.clone();
//                    allDownloadInfo.put(filePath, transferInfo);
//                    return transferInfo;
//                } catch (CloneNotSupportedException e) {
//                    e.printStackTrace();
//                }
//            }
        }
        //create a new instance if not found.
        downloadInfo = new TransferInfo(url, filePath);
        maxCreateTime++;
        downloadInfo.createTime = maxCreateTime;
        allDownloadInfo.put(downloadInfo.getFilePath(), downloadInfo);
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    public synchronized void submit(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        String filePath = downloadRequest.getFilePath();
        TransferInfo downloadInfo = getDownloadInfo(url, filePath);
        downloadRequest.setDownloadInfo(downloadInfo);
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        if (downloadTask != null && (readyTaskQueue.contains(downloadTask) || runningTaskQueue.contains(downloadTask))) {
            //The task is running,we need do nothing.
            LogUtil.e("task " + downloadInfo.getName() + " is running,we need do nothing.");
            return;
        }
        if (!downloadInfo.isFinished() || downloadRequest.isForceReDownload()) {
            if (downloadInfo.isFinished()) {
                downloadInfo.setFinished(0);
                downloadInfo.setCompletedSize(0);
            }
//            downloadInfo.calculateDownloadProgress();
//            downloadInfo.setTag(null);
            downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
//            if (downloadInfo.getDownloadFile().exists()) {
//                downloadInfo.getDownloadFile().delete();
//            }
            submitTask(downloadRequest);
        } else {
            downloadInfo.setErrorCode(ErrorCode.FILE_ALREADY_EXISTS);
            ServiceAgency.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
        }
    }

    private void submitTask(DownloadRequest downloadRequest) {
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
        String filePath = downloadInfo.getFilePath();
        if (allDownloadInfo.containsKey(filePath)) {
            allDownloadInfo.remove(filePath);
            if (readyTaskQueue.contains(downloadInfo)) {
                readyTaskQueue.remove(downloadInfo);
            }
            synchronized (downloadInfo) {
                TransferInfo transferInfo = (TransferInfo) downloadInfo;
                DownloadTask downloadTask = transferInfo.getDownloadTask();
                if (downloadTask != null) {
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
        TransferInfo transferInfo = (TransferInfo) downloadInfo;
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
        TransferInfo transferInfo = (TransferInfo) downloadInfo;
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null && downloadTask.getRequest() != null) {
            DownloadRequest downloadRequest = downloadTask.getRequest();
            submit(downloadRequest);
        }
    }

    @Override
    public List<TransferInfo> getDownloadingList() {
        List<TransferInfo> downloadList = new ArrayList<>();
        for (TransferInfo info : allDownloadInfo.values()) {
            if (!info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<TransferInfo> getDownloadedList() {
        List<TransferInfo> downloadList = new ArrayList<>();
        for (TransferInfo info : allDownloadInfo.values()) {
            if (info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<TransferInfo> getAllDownloadList() {
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
        context.stopService(new Intent(context, DownloadService.class));
        for (TransferInfo transferInfo : allDownloadInfo.values()) {
            stop(transferInfo);
        }
        DownloadInfoSnapshot.release();
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
        TransferInfo downloadInfo = downloadTask.getDownloadInfo();
        LogUtil.d("Task " + downloadInfo.getName() + " is stopped.");
        runningTaskQueue.remove(downloadTask);
        semaphore.release();
    }
}
