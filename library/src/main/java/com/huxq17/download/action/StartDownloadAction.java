package com.huxq17.download.action;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.TaskManager;
import com.huxq17.download.task.DownloadBlockTask;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.SimpleDownloadTask;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class StartDownloadAction implements Action {
    private DownloadRequest downloadRequest;
    private DownloadChain downloadChain;
    private DownloadTask downloadTask;
    private DownloadDetailsInfo downloadInfo;
    private long fileLength;
    private File tempDir;
    private String url;

    @Override
    public boolean proceed(DownloadChain chain) {
        downloadChain = chain;
        boolean result = true;
        downloadTask = chain.getDownloadTask();
        boolean preIsDowngrade = downloadTask.isDowngrade();
        downloadInfo = downloadTask.getDownloadInfo();
        downloadRequest = downloadTask.getRequest();
        url = downloadInfo.getUrl();
        fileLength = downloadInfo.getContentLength();
        tempDir = downloadInfo.getTempDir();
        if (!downloadTask.shouldStop()) {
            if (fileLength > 0) {
                result = downloadWithKnownContentLength();
            } else {
                result = downloadWithUnknownContentLength();
            }
        } else {
            return false;
        }
        if (downloadTask.isDowngrade() && !preIsDowngrade) {
            result = false;
        }
        if (downloadInfo.getErrorCode() == ErrorCode.NETWORK_UNAVAILABLE) {
            result = false;
        }

        return result;
    }

    private boolean downloadWithUnknownContentLength() {
        SimpleDownloadTask simpleDownloadTask = new SimpleDownloadTask(downloadChain);
        downloadTask.addBlockTask(simpleDownloadTask);
        simpleDownloadTask.run();
        return true;
    }

    private boolean downloadWithKnownContentLength() {
        CountDownLatch countDownLatch;
        long completedSize = 0;
        int threadNum = downloadRequest.getThreadNum();
        countDownLatch = new CountDownLatch(threadNum);
        synchronized (downloadTask.getLock()) {
            for (int i = 0; i < threadNum; i++) {
                DownloadBatch batch = new DownloadBatch();
                batch.threadId = i;
                batch.calculateStartPos(fileLength, threadNum);
                batch.calculateEndPos(fileLength, threadNum);
                completedSize += batch.calculateCompletedPartSize(tempDir);
                batch.url = url;
                DownloadBlockTask task = new DownloadBlockTask(batch, countDownLatch, downloadChain);
                downloadTask.addBlockTask(task);
                TaskManager.execute(task);
            }
        }
        downloadInfo.setCompletedSize(completedSize);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            //ignore.
            return false;
        }
        return true;
    }
}