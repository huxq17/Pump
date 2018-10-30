package com.huxq17.download.task;


import android.util.Log;

import com.huxq17.download.DownloadBatch;
import com.huxq17.download.Utils.Util;

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
    private DownloadTask downloadTask;
    private CountDownLatch countDownLatch;

    public DownloadBlockTask(DownloadBatch batch, CountDownLatch countDownLatch, DownloadTask downloadTask) {
        this.batch = batch;
        this.downloadTask = downloadTask;
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
//                Map<String, List<String>> headers = conn.getHeaderFields();
//                Set<Map.Entry<String, List<String>>> sets = headers.entrySet();
//                for (Map.Entry<String, List<String>> entry : sets) {
//                    String key = entry.getKey();
//                    if (entry.getValue() != null)
//                        for (String value : entry.getValue()) {
//                            Log.e("tag", "threadId=" + batch.threadId + ";head key=" + key + ";value=" + value);
//                        }
//                }
                //TODO 有的网站不允许断点下载，则返回code就不是206，不知道是不是因为请求的时候userAgent是android的原因，下次带pc的试试。
                if (conn.getResponseCode() == 206) {
                    inputStream = conn.getInputStream();
                    byte[] buffer = new byte[8092 * 10];
                    int len;
                    //TODO 写入文件的时候可以尝试用MappedByteBuffer共享内存优化。 用okio优化比较下
                    fileOutputStream = new FileOutputStream(tempFile, true);
                    int sum = 0;
                    while (!downloadTask.isStopped() && (len = inputStream.read(buffer)) != -1) {
                        fileOutputStream.write(buffer, 0, len);
                        sum += len;
                        downloadTask.onDownload(len);
                    }
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
