package com.huxq17.download.task;

import android.util.Log;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.SpeedMonitor;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.TaskManager;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.action.GetFileSizeAction;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.message.IMessageCenter;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class DownloadTask implements Task {
    private TransferInfo downloadInfo;
    private DBService dbService;
    private long completedSize;
    private boolean isStopped;
    private IMessageCenter messageCenter;
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private SpeedMonitor speedMonitor;

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

    private void download() {
        start = System.currentTimeMillis();
        String url = downloadInfo.getUrl();
        GetFileSizeAction getFileSizeAction = new GetFileSizeAction();
        long fileLength = getFileSizeAction.proceed(downloadInfo);
        if (fileLength == -1) {
            notifyProgressChanged(downloadInfo);
            return;
        }
        File tempDir = downloadInfo.getTempDir();
        long localLength = dbService.queryLocalLength(downloadInfo);
        if (fileLength != localLength) {
            //If file's length have changed,we need to re-download it.
            Util.deleteDir(tempDir);
        }
        downloadInfo.setFinished(0);
        downloadInfo.setCompletedSize(0);
        downloadInfo.setContentLength(fileLength);
        dbService.updateInfo(downloadInfo);
        int threadNum = downloadInfo.threadNum;
        String[] childList = tempDir.list();
        if (childList != null && childList.length != threadNum) {
            Util.deleteDir(tempDir);
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        CountDownLatch countDownLatch;
        countDownLatch = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++) {
            DownloadBatch batch = new DownloadBatch();
            batch.threadId = i;
            batch.calculateStartPos(fileLength, threadNum);
            batch.calculateEndPos(fileLength, threadNum);
            completedSize += batch.calculateCompletedPartSize(tempDir);
            batch.url = url;
            DownloadBlockTask task = new DownloadBlockTask(batch, countDownLatch, this);
            TaskManager.execute(task);
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        File file = downloadInfo.getDownloadFile();
        if (file.exists()) {
            file.delete();
        }
        if (!downloadInfo.isNeedDelete()) {
            if (completedSize == fileLength) {
                end = System.currentTimeMillis();
                Log.e("tag", "download spend=" + (end - start));

                File[] downloadPartFiles = tempDir.listFiles();
                if (downloadPartFiles != null && downloadPartFiles.length > 0) {
                    Util.mergeFiles(downloadPartFiles, file);
                    Util.deleteDir(tempDir);
                }
                Log.e("tag", "merge files spend=" + (System.currentTimeMillis() - end));
                downloadInfo.setFinished(1);
                downloadInfo.setCompletedSize(completedSize);
                dbService.updateInfo(downloadInfo);
                downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                notifyProgressChanged(downloadInfo);
            } else {
                if (!isStopped) {
                    DownloadInfo.Status status = downloadInfo.getStatus();
                    if (status == DownloadInfo.Status.PAUSING) {
                        downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
                    } else {
                        downloadInfo.setStatus(DownloadInfo.Status.FAILED);
                    }
                    notifyProgressChanged(downloadInfo);
                }
            }
        }
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


    private void notifyProgressChanged(TransferInfo downloadInfo) {
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
        isStopped = true;
        downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
        messageCenter = null;
    }

    public void setErrorCode(int errorCode) {
        if (downloadInfo.getStatus() != DownloadInfo.Status.PAUSING) {
            downloadInfo.setErrorCode(errorCode);
        }
    }

    public boolean shouldStop() {
        DownloadInfo.Status status = downloadInfo.getStatus();
        return status != DownloadInfo.Status.RUNNING || isStopped || downloadInfo.isNeedDelete();
    }
}
