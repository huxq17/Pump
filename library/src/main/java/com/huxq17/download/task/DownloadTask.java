package com.huxq17.download.task;

import android.util.Log;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.SpeedMonitor;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.action.Action;
import com.huxq17.download.action.CorrectDownloadInfoAction;
import com.huxq17.download.action.GetContentLengthAction;
import com.huxq17.download.action.MergeFileAction;
import com.huxq17.download.action.StartDownloadAction;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.message.IMessageCenter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DownloadTask implements Task {
    private TransferInfo downloadInfo;
    private DBService dbService;
    private long completedSize;
    private boolean isStopped;
    private IMessageCenter messageCenter;
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private SpeedMonitor speedMonitor;
    CountDownLatch countDownLatch;

    public DownloadTask(TransferInfo downloadInfo, DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        downloadInfo.setDownloadTask(this);
        this.downloadInfo = downloadInfo;
        completedSize = 0l;
        isStopped = false;
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
        if (!isStopped) {
            downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
        }
        downLoadLifeCycleObserver.onDownloadStart(this);
        if (!downloadInfo.isNeedDelete() && !shouldStop()) {
            download();
        }
        downLoadLifeCycleObserver.onDownloadEnd(this);
    }

    private void log(String msg) {
        Log.e("tag", msg);
    }

    private void downloadWithDownloadChain() {
        List<Action> actions = new ArrayList<>();
        actions.add(new GetContentLengthAction());
        actions.add(new CorrectDownloadInfoAction());
        actions.add(new StartDownloadAction());
        actions.add(new MergeFileAction());
        DownloadChain chain = new DownloadChain(this, actions);
        chain.proceed();
    }

    private void download() {
        start = System.currentTimeMillis();
        downloadWithDownloadChain();
        end = System.currentTimeMillis();
        log("download spend=" + (end - start));
    }

    private int lastProgress = 0;

    public synchronized void onDownload(int length) {
        this.completedSize += length;
        downloadInfo.setCompletedSize(this.completedSize);
        speedMonitor.compute(length);
        int progress = (int) (completedSize * 1f / downloadInfo.getContentLength() * 100);
        if (progress != lastProgress) {
            lastProgress = progress;
            if (progress != 100) {
                notifyProgressChanged(downloadInfo);
            }
        }
    }

    public void setCountDownLatch(CountDownLatch countDownLatch) {
        this.countDownLatch = countDownLatch;
    }

    public void notifyProgressChanged(TransferInfo downloadInfo) {
        if (messageCenter != null)
            messageCenter.notifyProgressChanged(downloadInfo);
    }


    public TransferInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void pause() {
        downloadInfo.setStatus(DownloadInfo.Status.PAUSING);
        notifyProgressChanged(downloadInfo);
    }

    public void stop() {
        Date date = new Date();
        Log.e("tag", "stop task name=" + downloadInfo.getName() + "  at " + date.toString());
        isStopped = true;
        downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
        downloadInfo.setDownloadTask(null);
        messageCenter = null;
        if (countDownLatch != null) {
            long count = countDownLatch.getCount();
            for (int i = 0; i < count; i++) {
                countDownLatch.countDown();
            }
        }
    }

    public boolean isDestroy() {
        return isStopped;
    }

    public void setErrorCode(int errorCode) {
        if (downloadInfo.getStatus() != DownloadInfo.Status.PAUSING) {
            downloadInfo.setErrorCode(errorCode);
        }
    }

    public void updateInfo(TransferInfo transferInfo) {
        synchronized (transferInfo) {
            if (!transferInfo.isNeedDelete()) {
                dbService.updateInfo(transferInfo);
            }
        }
    }

    public boolean shouldStop() {
        DownloadInfo.Status status = downloadInfo.getStatus();
        return status != DownloadInfo.Status.RUNNING || isStopped || downloadInfo.isNeedDelete();
    }
}
