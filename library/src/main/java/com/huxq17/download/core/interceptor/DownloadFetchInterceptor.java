package com.huxq17.download.core.interceptor;

import com.huxq17.download.TaskManager;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.task.DownloadBlockTask;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.core.task.SimpleDownloadTask;

import java.util.concurrent.CountDownLatch;

public class DownloadFetchInterceptor implements DownloadInterceptor {
    private DownloadDetailsInfo downloadInfo;
    private DownloadRequest downloadRequest;
    private DownloadTask downloadTask;

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        downloadRequest = chain.request();
        downloadInfo = downloadRequest.getDownloadInfo();
        downloadTask = downloadInfo.getDownloadTask();
        if (downloadInfo.isRunning()) {
            if (downloadInfo.isSupportBreakpoint()) {
                downloadWithBreakpoint();
            } else {
                downloadWithoutBreakPoint();
            }
        } else {
            return downloadInfo.snapshot();
        }
        if (!downloadInfo.isRunning()) {
            return downloadInfo.snapshot();
        }
        return chain.proceed(downloadRequest);
    }

    private boolean downloadWithoutBreakPoint() {
        SimpleDownloadTask simpleDownloadTask = new SimpleDownloadTask(downloadRequest);
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
                DownloadBlockTask task = new DownloadBlockTask(downloadRequest, countDownLatch, i);
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
