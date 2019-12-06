package com.huxq17.download.core.action;


import com.huxq17.download.DownloadChain;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.TaskManager;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.task.DownloadBlockTask;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.core.task.SimpleDownloadTask;

import java.util.concurrent.CountDownLatch;

public class DownloadExecuteAction implements Action {
    private DownloadRequest downloadRequest;
    private DownloadChain downloadChain;
    private DownloadTask downloadTask;
    private DownloadDetailsInfo downloadInfo;

    @Override
    public boolean proceed(DownloadChain chain) {
        downloadChain = chain;
        boolean result;
        downloadTask = chain.getDownloadTask();
        downloadInfo = downloadTask.getDownloadInfo();
        downloadRequest = downloadTask.getRequest();
        if (downloadTask.isRunning()) {
            if (downloadTask.isSupportBreakpoint()) {
                result = downloadWithBreakpoint();
            } else {
                result = downloadWithoutBreakPoint();
            }
        } else {
            return false;
        }
        if (downloadInfo.getErrorCode() == ErrorCode.NETWORK_UNAVAILABLE) {
            result = false;
        }
        return result;
    }

    private boolean downloadWithoutBreakPoint() {
        SimpleDownloadTask simpleDownloadTask = new SimpleDownloadTask(downloadChain);
        downloadTask.addBlockTask(simpleDownloadTask);
        simpleDownloadTask.run();
        return true;
    }

    private boolean downloadWithBreakpoint() {
        CountDownLatch countDownLatch;
        long completedSize = 0;
        int threadNum = downloadRequest.getThreadNum();
        countDownLatch = new CountDownLatch(threadNum);
        synchronized (downloadTask.getLock()) {
            for (int i = 0; i < threadNum; i++) {
                DownloadBlockTask task = new DownloadBlockTask(downloadChain, countDownLatch, i);
                completedSize += task.getCompletedSize();
                downloadTask.addBlockTask(task);
                TaskManager.execute(task);
            }
        }
        downloadInfo.setCompletedSize(completedSize);
        try {
            countDownLatch.await();
        } catch (InterruptedException ignore) {
            return false;
        }
        return true;
    }
}