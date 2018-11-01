package com.huxq17.download.manager;


import android.content.Context;
import android.content.Intent;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.TransferInfo;
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
    private int maxRunningTaskNumber;
    private boolean isServiceRunning = false;
    private DownloadConfig downloadConfig;
    private HashMap<String, TransferInfo> allDownloadInfo = new LinkedHashMap<>();

    private DownloadManager() {
        List<TransferInfo> allDownloadInfo = DBService.getInstance().getDownloadList();
        for (TransferInfo transferInfo : allDownloadInfo) {
            this.allDownloadInfo.put(transferInfo.getFilePath(), transferInfo);
        }
    }

    private TransferInfo getDownloadInfo(String url, String filePath) {
        TransferInfo downloadInfo = allDownloadInfo.get(filePath);
        if (downloadInfo != null) {
            if (!downloadInfo.isUsed()) {
                return downloadInfo;
            } else {
                try {
                    TransferInfo transferInfo = downloadInfo.clone();
                    allDownloadInfo.put(filePath, transferInfo);
                    return transferInfo;
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
        // create a new instance if not found.
        downloadInfo = new TransferInfo(url, filePath);
        downloadInfo.createTime = allDownloadInfo.size();
        allDownloadInfo.put(downloadInfo.getFilePath(), downloadInfo);
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    public void submit(String url, String filePath) {
        TransferInfo downloadInfo = getDownloadInfo(url, filePath);
        if (!downloadInfo.isFinished() || downloadConfig.forceReDownload) {
            downloadInfo.setFinished(0);
            downloadInfo.setCompletedSize(0);
            downloadInfo.setTag(null);
            downloadInfo.setStatus(DownloadInfo.Status.WAIT);
            if (downloadInfo.getDownloadFile().exists()) {
                downloadInfo.getDownloadFile().delete();
            }
            submit(downloadInfo);
        } else {
            downloadInfo.setErrorCode(ErrorCode.FILE_ALREADY_EXISTS);
            ServiceAgency.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
        }
    }

    private void submit(TransferInfo downloadInfo) {
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

    public void delete(DownloadInfo downloadInfo) {
        if (downloadInfo == null) return;
        String filePath = downloadInfo.getFilePath();
        if (allDownloadInfo.containsKey(filePath)) {
            allDownloadInfo.remove(filePath);
            if (readyTaskQueue.contains(downloadInfo)) {
                readyTaskQueue.remove(downloadInfo);
            } else {
                synchronized (runningTaskQueue) {
                    TransferInfo transferInfo = (TransferInfo) downloadInfo;
                    transferInfo.setNeedDelete(true);
                    transferInfo.getDownloadFile().delete();
                }
            }
            DBService.getInstance().deleteInfo(downloadInfo.getUrl(), downloadInfo.getFilePath());
        }
    }

    @Override
    public void stop(DownloadInfo downloadInfo) {
        synchronized (downloadInfo) {
            DownloadTask downloadTask = ((TransferInfo) downloadInfo).getDownloadTask();
            if (downloadTask != null) {
                downloadTask.stop();
            }
        }
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
    public void reStart(DownloadInfo downloadInfo) {
        submit((TransferInfo) downloadInfo);
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
        this.downloadConfig = downloadConfig;
    }

    @Override
    public void shutdown() {
        context.stopService(new Intent(context, DownloadService.class));
    }

    public DownloadTask acquireTask() throws InterruptedException {
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
        TransferInfo downloadInfo = downloadTask.getDownloadInfo();
        synchronized (runningTaskQueue) {
            if (downloadInfo.isNeedDelete()) {
                Util.deleteDir(downloadInfo.getTempDir());
                downloadInfo.getDownloadFile().delete();
                DBService.getInstance().deleteInfo(downloadInfo.getUrl(), downloadInfo.getFilePath());
            }
        }
        runningTaskQueue.remove(downloadTask);
        downloadInfo.setDownloadTask(null);
        semaphore.release();
    }
}
