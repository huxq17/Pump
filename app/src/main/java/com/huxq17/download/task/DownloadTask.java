package com.huxq17.download.task;

import android.util.Log;

import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.action.GetFileSizeAction;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownloadStatus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DownloadTask implements Task, DownloadStatus {
    private DownloadInfo downloadInfo;
    private DBService dbService;
    private long completeSize;
    private boolean isStopped;

    public DownloadTask(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        completeSize = 0l;
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
        int threadNum = downloadInfo.threadNum;
        downloadInfo.contentLength = fileLength;
        File tempFile = downloadInfo.getTempFile();
        if (fileLength != dbService.queryLocalLength(downloadInfo)) {
            //If file's length have changed,we need to re-download it.
            downloadInfo.finished = 0;
            if (tempFile.exists()) {
                tempFile.delete();
            }
        } else {
            if (downloadInfo.isFinished() && !downloadInfo.forceRestart) {
                //TODO 已经下完过了，无需重复下载.
                return;
            }
        }
        List<DownloadBatch> batches = null;
        if (!tempFile.exists()) {
            dbService.deleteBatchByUrl(url);
            dbService.updateInfo(downloadInfo);
            RandomAccessFile raf = null;
            try {
                if (!tempFile.getParentFile().exists()) {
                    tempFile.getParentFile().mkdirs();
                }
                tempFile.createNewFile();
                raf = new RandomAccessFile(tempFile, "rwd");
                raf.setLength(fileLength);
            } catch (FileNotFoundException e) {
                //TODO download failed.
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                //TODO download failed.
            } finally {
                Util.closeQuietly(raf);
            }
        } else {
            batches = dbService.queryLocalBatch(url);
        }
        //
        CountDownLatch countDownLatch;
        if (batches != null && batches.size() > 0) {
            threadNum = batches.size();
            countDownLatch = new CountDownLatch(threadNum);
            for (DownloadBatch batch : batches) {
                completeSize += batch.downloadedSize;
                batch.tempFile = tempFile;
                batch.calcuStartPos(fileLength, threadNum);
                batch.calcuEndPos(fileLength, threadNum);
                DownloadBlockTask task = new DownloadBlockTask(batch, countDownLatch, this);
                TaskManager.execute(task);
            }
        } else {
            countDownLatch = new CountDownLatch(threadNum);
            for (int i = 0; i < threadNum; i++) {
                DownloadBatch batch = new DownloadBatch();
                batch.threadId = i;
                batch.calcuStartPos(fileLength, threadNum);
                batch.calcuEndPos(fileLength, threadNum);
                batch.tempFile = tempFile;
                batch.url = url;
                DownloadBlockTask task = new DownloadBlockTask(batch, countDownLatch, this);
                TaskManager.execute(task);
            }
        }
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (completeSize == fileLength) {
            //10*8012 28584 28800 28437
            //8012    56443 66582 55008
            end = System.currentTimeMillis();
            Log.e("tag", "download finished spend=" + (end - start));
            File file = new File(downloadInfo.filePath);
            if (file.exists()) {
                boolean fileDelete = file.delete();
                Log.e("tag", "fileDelete=" + fileDelete);
            }
            File downloadTempFile = downloadInfo.getTempFile();
            boolean renameResult = downloadTempFile.renameTo(file);
            Log.e("tag", "renameResult=" + renameResult);
            dbService.deleteBatchByUrl(downloadInfo.url);
            downloadInfo.finished = 1;
            dbService.updateInfo(downloadInfo);
//            dbService.deleteInfoByUrl(downloadInfo.url);
        } else {
            Log.e("tag", "download failed.");
        }
//        ResumeDownloadAction resumeDownloadAction = new ResumeDownloadAction();
//        resumeDownloadAction.proceed(url);
    }

    @Override
    public synchronized void onDownload(int threadId, int length, long downloadedSize) {
        long fileLength = downloadInfo.contentLength;
        this.completeSize += length;
        int progress = (int) (completeSize * 1f / downloadInfo.contentLength * 100);
        Log.e("tag", "download progress=" + progress);
        dbService.updateBatch(downloadInfo.url, threadId, downloadedSize);
    }

    public void stop() {
        isStopped = true;
    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }
}
