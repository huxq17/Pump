package com.huxq17.download.core.task;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.connection.DownloadConnection;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;


public class DownloadBlockTask implements Task {
    private DownloadBatch batch;
    private DownloadChain downloadChain;
    private CountDownLatch countDownLatch;
    private volatile boolean isCanceled;
    private DownloadConnection connection;

    public DownloadBlockTask(DownloadBatch batch, CountDownLatch countDownLatch, DownloadChain downloadChain) {
        this.batch = batch;
        this.downloadChain = downloadChain;
        this.countDownLatch = countDownLatch;
        connection = PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(batch.url);
    }

    @Override
    public void run() {
        long downloadedSize = batch.downloadedSize;
        long startPosition = batch.startPos + downloadedSize;
        long endPosition = batch.endPos;
        File tempFile = batch.tempFile;
        DownloadTask downloadTask = downloadChain.getDownloadTask();
        if (startPosition != endPosition + 1) {
            connection.addHeader("Range", "bytes=" + startPosition + "-" + endPosition);
            try {
                connection.connect();
                int code = connection.getResponseCode();
                if (!isCanceled) {
                    if (code == HttpURLConnection.HTTP_PARTIAL) {
                        int len;
                        connection.prepareDownload(tempFile);
                        byte[] buffer = new byte[8092];
                        while (!isCanceled && (len = connection.downloadBuffer(buffer)) != -1) {
                            if (!downloadTask.onDownload(len)) {
                                break;
                            }
                        }
                        connection.flushDownload();
                    } else {
                        downloadTask.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
                        downloadTask.cancelBlockTasks();
                    }
                }
            } catch (IOException e) {
                if (!connection.isCanceled()) {
                    e.printStackTrace();
                    downloadTask.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
                    downloadTask.cancelBlockTasks();
                }
            } finally {
                connection.close();
            }
        }
        countDownLatch.countDown();
        this.batch = null;
        this.downloadChain = null;
        this.countDownLatch = null;
    }

    @Override
    public void cancel() {
        isCanceled = true;
        connection.cancel();
    }
}
