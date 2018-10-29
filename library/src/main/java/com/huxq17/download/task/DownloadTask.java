package com.huxq17.download.task;

import android.util.Log;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.DownloadBatch;
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
    }


    private long start, end;

    @Override
    public void run() {
        downLoadLifeCycleObserver.onDownloadStart(this);
        download();
        downLoadLifeCycleObserver.onDownloadEnd(this);
    }

    private void download() {
        start = System.currentTimeMillis();
        String url = downloadInfo.getUrl();
        GetFileSizeAction getFileSizeAction = new GetFileSizeAction();
        long fileLength = getFileSizeAction.proceed(url);
        downloadInfo.setContentLength(fileLength);
        File tempDir = downloadInfo.getTempDir();
        long localLength = dbService.queryLocalLength(downloadInfo);
        if (fileLength != localLength) {
            //If file's length have changed,we need to re-download it.
            Util.deleteDir(tempDir);
        } else {
            if (downloadInfo.isFinished() && !downloadInfo.forceReDownload) {
                //TODO 已经下完过了，无需重复下载,提示用户
                return;
            }
        }
        downloadInfo.setFinished(0);
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
            downloadInfo.setProgress(100);
            dbService.updateInfo(downloadInfo);
            notifyProgressChanged(downloadInfo);
//            dbService.deleteInfoByUrl(downloadInfo.url);
        } else {
            Log.e("tag", "download failed.");
        }
    }

    private int lastProgress = 0;

    public synchronized void onDownload(int length) {
        this.completedSize += length;
        downloadInfo.setCompletedSize(this.completedSize);
        int progress = (int) (completedSize * 1f / downloadInfo.getContentLength() * 100);
        if (progress != lastProgress) {
//            Log.e("tag", "download progress=" + progress);
            lastProgress = progress;
            downloadInfo.setProgress(progress);
            if (progress != 100) {
                notifyProgressChanged(downloadInfo);
            }
        }
    }

    private void notifyProgressChanged(TransferInfo downloadInfo) {
        messageCenter.notifyProgressChanged(downloadInfo);
    }

    public void stop() {
        isStopped = true;
    }

    public TransferInfo getDownloadInfo() {
        return downloadInfo;
    }

    public boolean isStopped() {
        return isStopped;
    }
}
