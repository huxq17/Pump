package com.huxq17.download.task;

import android.util.Log;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadInfo;
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

    public DownloadTask(TransferInfo downloadInfo, DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downloadInfo = downloadInfo;
        completedSize = 0l;
        isStopped = false;
        dbService = DBService.getInstance();
        messageCenter = ServiceAgency.getService(IMessageCenter.class);
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
        downloadInfo.setStatus(DownloadInfo.Status.WAIT);
        notifyProgressChanged(downloadInfo);
    }

    private long start, end;

    @Override
    public void run() {
        downLoadLifeCycleObserver.onDownloadStart(this);
        download();
        downLoadLifeCycleObserver.onDownloadEnd(this);
    }

    private void log(String msg) {
        Log.e("tag", msg);
    }

    private void download() {
        downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
        start = System.currentTimeMillis();
        String url = downloadInfo.getUrl();
        GetFileSizeAction getFileSizeAction = new GetFileSizeAction();
        long fileLength = getFileSizeAction.proceed(url);
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
        if (completedSize == fileLength) {
            end = System.currentTimeMillis();
            Log.e("tag", "download spend=" + (end - start));
            File file = new File(downloadInfo.getFilePath());
            if (file.exists()) {
                file.delete();
            }
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
//            dbService.deleteInfoByUrl(downloadInfo.url);
        } else {
            if (downloadInfo.getStatus() == DownloadInfo.Status.RUNNING) {
                downloadInfo.setStatus(DownloadInfo.Status.FAILED);
                notifyProgressChanged(downloadInfo);
            }
            Log.e("tag", "download failed.");
        }
    }

    private int lastProgress = 0;

    public synchronized void onDownload(int length) {
        this.completedSize += length;
        downloadInfo.setCompletedSize(this.completedSize);
        computeSpeed(length);
        int progress = (int) (completedSize * 1f / downloadInfo.getContentLength() * 100);
        if (progress != lastProgress) {
            lastProgress = progress;
            if (progress != 100) {
                notifyProgressChanged(downloadInfo);
            }
        }
    }

    private long totalRead = 0;
    private long lastSpeedCountTime = 0;
    final double NANOS_PER_SECOND = 1000000000.0;  //1秒=10亿nanoseconds
    final double BYTES_PER_MIB = 1024 * 1024;    //1M=1024*1024byte
    final double BYTES_PER_KB = 1024;
    final String BYTE_SUFFIX = "B/s";
    final String KB_SUFFIX = "KB/s";
    final String MIB_SUFFIX = "M/s";

    private void computeSpeed(int length) {
        totalRead += length;
        long curTime = System.nanoTime();
        if (lastSpeedCountTime == 0) {
            lastSpeedCountTime = curTime;
        }
        if (curTime >= lastSpeedCountTime + NANOS_PER_SECOND) {
            double speed = 0;
            String suffix = "";
            if (totalRead < BYTES_PER_KB) {
                speed = NANOS_PER_SECOND * totalRead / (curTime - lastSpeedCountTime);
                suffix = BYTE_SUFFIX;
            } else if (totalRead >= BYTES_PER_KB && totalRead < BYTES_PER_MIB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_KB / (curTime - lastSpeedCountTime);
                suffix = KB_SUFFIX;
            } else if (totalRead >= BYTES_PER_MIB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_MIB / (curTime - lastSpeedCountTime);
                suffix = MIB_SUFFIX;
            }
            speed = (double) Math.round(speed * 100) / 100;
            downloadInfo.setSpeed(speed + suffix);
            lastSpeedCountTime = curTime;
            totalRead = 0;
        }
    }

    private void notifyProgressChanged(TransferInfo downloadInfo) {
        messageCenter.notifyProgressChanged(downloadInfo);
    }


    public TransferInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void stop() {
        downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
        notifyProgressChanged(downloadInfo);
    }

    public boolean isStopped() {
        return downloadInfo.getStatus() == DownloadInfo.Status.STOPPED;
    }
}
