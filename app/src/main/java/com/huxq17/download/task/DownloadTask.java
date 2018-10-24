package com.huxq17.download.task;

import android.util.Log;

import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.action.GetFileSizeAction;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownloadStatus;

import java.io.File;
import java.util.concurrent.CountDownLatch;

public class DownloadTask implements Task, DownloadStatus {
    private DownloadInfo downloadInfo;
    private DBService dbService;
    private long completedSize;
    private boolean isStopped;

    public DownloadTask(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        completedSize = 0l;
        isStopped = false;
        dbService = DBService.getInstance();
    }

    private long start, end;

    @Override
    public void run() {
        start = System.currentTimeMillis();
        String url = downloadInfo.url;
        GetFileSizeAction getFileSizeAction = new GetFileSizeAction();
        long fileLength = getFileSizeAction.proceed(url);
        downloadInfo.contentLength = fileLength;
        File tempDir = downloadInfo.getTempDir();
        long localLength = dbService.queryLocalLength(downloadInfo);
        Log.e("tag", "fileLength=" + fileLength + ";localLength=" + localLength);
        if (fileLength != localLength) {
            //If file's length have changed,we need to re-download it.
            downloadInfo.finished = 0;
            Util.deleteDir(tempDir);
        } else {
            if (downloadInfo.isFinished() && !downloadInfo.forceRestart) {
                //TODO 已经下完过了，无需重复下载.
                return;
            }
        }
        int threadNum = downloadInfo.threadNum;
        String[] childList = tempDir.list();
        if (childList != null && childList.length != threadNum) {
            Util.deleteDir(tempDir);
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        dbService.updateInfo(downloadInfo);
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
            Log.e("tag", "download finished spend=" + (end - start));
            File file = new File(downloadInfo.filePath);
            if (file.exists()) {
                file.delete();
            }
            File[] downloadPartFiles = tempDir.listFiles();
            if (downloadPartFiles != null && downloadPartFiles.length > 0) {
                Util.mergeFiles(downloadPartFiles, file);
                Util.deleteDir(tempDir);
            }
            downloadInfo.finished = 1;
            dbService.updateInfo(downloadInfo);
//            dbService.deleteInfoByUrl(downloadInfo.url);
        } else {
            Log.e("tag", "download failed.");
        }
//        ResumeDownloadAction resumeDownloadAction = new ResumeDownloadAction();
//        resumeDownloadAction.proceed(url);
    }

    private int lastProgress = 0;

    @Override
    public synchronized void onDownload(int threadId, int length, long downloadedSize) {
        this.completedSize += length;
        int progress = (int) (completedSize * 1f / downloadInfo.contentLength * 100);
        if (progress != lastProgress) {
            Log.e("tag", "download progress=" + progress);
            lastProgress = progress;
        }
    }

    public void stop() {
        isStopped = true;
    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }
}
