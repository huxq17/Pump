package com.huxq17.download.task;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.SpeedMonitor;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.message.IMessageCenter;

import java.util.ArrayList;
import java.util.List;

public class DownloadTask implements Task {
    private TransferInfo downloadInfo;
    private DBService dbService;
    private boolean isDestroyed;
    private boolean isNeedDelete;
    private IMessageCenter messageCenter;
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private SpeedMonitor speedMonitor;
    private Thread thread;
    private List<Task> downloadBlockTasks = new ArrayList<>();

    public DownloadTask(TransferInfo downloadInfo, DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        downloadInfo.setDownloadTask(this);
        this.downloadInfo = downloadInfo;
        isDestroyed = false;
        dbService = DBService.getInstance();
        downloadInfo.setUsed(true);
        speedMonitor = new SpeedMonitor(downloadInfo);
        messageCenter = ServiceAgency.getService(IMessageCenter.class);
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
        downloadInfo.setStatus(DownloadInfo.Status.WAIT);
        notifyProgressChanged(downloadInfo);
    }

    private long start, end;

    @Override
    public void run() {
        thread = Thread.currentThread();
        if (!isDestroyed) {
            downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
            notifyProgressChanged(downloadInfo);
        }
        downLoadLifeCycleObserver.onDownloadStart(this);
        if (!shouldStop()) {
            start = System.currentTimeMillis();
            downloadWithDownloadChain();
            end = System.currentTimeMillis();
            LogUtil.d("download spend=" + (end - start));
        }
        thread = null;
        downLoadLifeCycleObserver.onDownloadEnd(this);
    }

    private void downloadWithDownloadChain() {
        DownloadChain chain = new DownloadChain(this);
        chain.proceed();
    }

    private int lastProgress = 0;

    public boolean onDownload(int length) {
        synchronized (downloadInfo) {
            if (isDestroyed) {
                return false;
            }
            downloadInfo.download(length);
            speedMonitor.compute(length);
            int progress = (int) (downloadInfo.getCompletedSize() * 1f / downloadInfo.getContentLength() * 100);
            if (progress != lastProgress) {
                if (progress != 100) {
                    lastProgress = progress;
                    notifyProgressChanged(downloadInfo);
                }
            }
        }
        return true;
    }

    public void notifyProgressChanged(TransferInfo downloadInfo) {
        if (messageCenter != null)
            messageCenter.notifyProgressChanged(downloadInfo);
    }


    public TransferInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void addBlockTask(Task task) {
        downloadBlockTasks.add(task);
    }

    public void pause() {
        synchronized (downloadInfo) {
            if (!isDestroyed) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSING);
                notifyProgressChanged(downloadInfo);
                cancel();
            }
        }
    }

    public void stop() {
        synchronized (downloadInfo) {
            if (!isDestroyed) {
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                downloadInfo.setDownloadTask(null);
                cancel();
            }
        }
    }

    public void cancel() {
        if (thread != null) {
            thread.interrupt();
        }
        for (Task task : downloadBlockTasks) {
            task.cancel();
        }
        destroy();
    }

    public void delete() {
        synchronized (downloadInfo) {
            if (!isDestroyed) {
                isNeedDelete = true;
                cancel();
            }
        }
    }

    public boolean isNeedDelete() {
        return isNeedDelete;
    }

    public void setErrorCode(int errorCode) {
        if (downloadInfo.getStatus() != DownloadInfo.Status.PAUSING) {
            downloadInfo.setErrorCode(errorCode);
        }
    }

    public void updateInfo(TransferInfo transferInfo) {
        synchronized (transferInfo) {
            if (!isNeedDelete) {
                dbService.updateInfo(transferInfo);
            }
        }
    }

    public void destroy() {
        isDestroyed = true;
    }

    public boolean shouldStop() {
        return isDestroyed;
    }

}
