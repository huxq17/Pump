package com.huxq17.download.task;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.listener.DownloadStatus;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CountDownLatch;


public class DownloadBlockTask implements Task {
    private DownloadBatch batch;
    private DownloadStatus downloadStatus;
    private CountDownLatch countDownLatch;

    public DownloadBlockTask(DownloadBatch batch, CountDownLatch countDownLatch, DownloadStatus downloadListener) {
        this.batch = batch;
        this.downloadStatus = downloadListener;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        HttpURLConnection conn = null;
        long downloadedSize = batch.downloadedSize;
        long startPosition = batch.startPos + downloadedSize;
        long endPosition = batch.endPos;
        File tempFile = batch.tempFile;
        BufferedOutputStream bufferedOutputStream = null;
        FileOutputStream fileOutputStream = null;
        if (startPosition != endPosition + 1) {
            InputStream inputStream = null;
            try {
                URL httpUrl = new URL(batch.url);
                conn = (HttpURLConnection) httpUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=" + startPosition + "-" + endPosition);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                inputStream = conn.getInputStream();
                byte[] buffer = new byte[8092 * 10];
                int len;
                fileOutputStream = new FileOutputStream(tempFile, true);
                int sum = 0;
                while (!downloadStatus.isStopped() && (len = inputStream.read(buffer)) != -1) {
                    fileOutputStream.write(buffer, 0, len);
                    sum += len;
                    downloadStatus.onDownload(batch.threadId, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
                //TODO download failed.
            } finally {
                Util.closeQuietly(inputStream);
                if (conn != null) {
                    conn.disconnect();
                }
                Util.closeQuietly(fileOutputStream);
                Util.closeQuietly(bufferedOutputStream);
            }
        }
        countDownLatch.countDown();
    }
}
