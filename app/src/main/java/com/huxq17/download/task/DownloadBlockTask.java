package com.huxq17.download.task;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.listener.DownloadStatus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;


public class DownloadBlockTask implements Task {
    private DownloadBatch batch;
    private DownloadStatus downloadStatus;

    public DownloadBlockTask(DownloadBatch batch, DownloadStatus downloadListener) {
        this.batch = batch;
        this.downloadStatus = downloadListener;
    }

    @Override
    public void run() {
        HttpURLConnection conn = null;
        RandomAccessFile raf = null;
        long downloadedSize = batch.downloadedSize;
        long startPosition = batch.startPos + downloadedSize;
        long endPosition = batch.endPos;
        File tempFile = batch.tempFile;
        if (startPosition == endPosition) {
            return;
        }
        try {
            URL httpUrl = new URL(batch.url);
            conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Range", "bytes=" + startPosition + "-" + endPosition);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            InputStream inputStream = conn.getInputStream();
            byte[] buffer = new byte[8092];
            int len;
            raf = new RandomAccessFile(tempFile, "rwd");
            raf.seek(startPosition);
            int sum = 0;
            while (!downloadStatus.isStopped() && (len = inputStream.read(buffer)) != -1) {
                raf.write(buffer, 0, len);
                sum += len;
                downloadStatus.onDownload(batch.threadId, len, sum + downloadedSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
            //TODO download failed.
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            Util.closeQuietly(raf);
        }
    }
}
