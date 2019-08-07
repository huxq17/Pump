package com.huxq17.download.action;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.TaskManager;
import com.huxq17.download.task.DownloadBlockTask;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class StartDownloadAction implements Action {

    @Override
    public boolean proceed(DownloadChain chain) {
        boolean result = true;
        DownloadTask downloadTask = chain.getDownloadTask();
        DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
        DownloadRequest downloadRequest = downloadTask.getRequest();
        String url = downloadInfo.getUrl();
        long fileLength = downloadInfo.getContentLength();
        int threadNum = downloadRequest.getThreadNum();
        File tempDir = downloadInfo.getTempDir();
        CountDownLatch countDownLatch;
        long completedSize = 0;
        countDownLatch = new CountDownLatch(threadNum);
        synchronized (downloadTask.getLock()) {
            if (!downloadTask.shouldStop()){
                for (int i = 0; i < threadNum; i++) {
                    DownloadBatch batch = new DownloadBatch();
                    batch.threadId = i;
                    batch.calculateStartPos(fileLength, threadNum);
                    batch.calculateEndPos(fileLength, threadNum);
                    completedSize += batch.calculateCompletedPartSize(tempDir);
                    batch.url = url;
                    DownloadBlockTask task = new DownloadBlockTask(batch, countDownLatch, chain);
                    downloadTask.addBlockTask(task);
                    TaskManager.execute(task);
                }
                downloadInfo.setCompletedSize(completedSize);
            }else{
                return false;
            }
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            //ignore.
            result = false;
        }
        if (downloadTask.isDowngrade()) {
            result = false;
        }
        if (downloadInfo.getErrorCode() == ErrorCode.NETWORK_UNAVAILABLE) {
            result = false;
        }

        return result;
    }
}