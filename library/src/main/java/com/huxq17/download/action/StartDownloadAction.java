package com.huxq17.download.action;

import android.util.Log;

import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TaskManager;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.task.DownloadBlockTask;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class StartDownloadAction implements Action {
    @Override
    public boolean proceed(DownloadTask t) {
        TransferInfo downloadInfo = t.getDownloadInfo();
        String url = downloadInfo.getUrl();
        long fileLength = downloadInfo.getContentLength();
        int threadNum = downloadInfo.threadNum;
        File tempDir = downloadInfo.getTempDir();
        CountDownLatch countDownLatch;
        long completedSize = 0;
        countDownLatch = new CountDownLatch(threadNum);
        t.setCountDownLatch(countDownLatch);
        for (int i = 0; i < threadNum; i++) {
            DownloadBatch batch = new DownloadBatch();
            batch.threadId = i;
            batch.calculateStartPos(fileLength, threadNum);
            batch.calculateEndPos(fileLength, threadNum);
            completedSize += batch.calculateCompletedPartSize(tempDir);
            batch.url = url;
            DownloadBlockTask task = new DownloadBlockTask(batch, countDownLatch, t);
            TaskManager.execute(task);
        }
        downloadInfo.setCompletedSize(completedSize);
        downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
        t.notifyProgressChanged(downloadInfo);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }
}