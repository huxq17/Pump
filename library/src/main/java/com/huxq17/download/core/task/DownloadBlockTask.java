package com.huxq17.download.core.task;


import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.connection.DownloadConnection;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static com.huxq17.download.utils.Util.DOWNLOAD_PART;


public class DownloadBlockTask implements Task {
    private CountDownLatch countDownLatch;
    private volatile boolean isCanceled;
    private DownloadConnection connection;
    private int blockId;
    private long completedSize;
    private File tempFile;
    private DownloadRequest downloadRequest;
    private DownloadDetailsInfo downloadInfo;

    public DownloadBlockTask(DownloadRequest downloadRequest, CountDownLatch countDownLatch, int blockId) {
        this.downloadRequest = downloadRequest;
        downloadInfo = downloadRequest.getDownloadInfo();
        connection = PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(downloadRequest.getUrl());
        this.countDownLatch = countDownLatch;
        this.blockId = blockId;
        calculateCompletedSize();
    }

    @Override
    public void run() {
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        long threadNum = downloadRequest.getThreadNum();
        long fileLength = downloadRequest.getDownloadInfo().getContentLength();
        long startPosition = blockId * fileLength / threadNum + completedSize;
        long endPosition;
        if (threadNum == blockId + 1) {
            endPosition = fileLength - 1;
        } else {
            endPosition = (blockId + 1) * fileLength / threadNum - 1;
        }

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
                        downloadInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
                        downloadTask.cancel();
                    }
                }
            } catch (IOException e) {
                if (!connection.isCanceled()) {
                    e.printStackTrace();
                    downloadInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
                    downloadTask.cancel();
                }
            } finally {
                connection.close();
            }
        }
        countDownLatch.countDown();
    }

    private void calculateCompletedSize() {
        File tempDir = downloadInfo.getTempDir();
        tempFile = new File(tempDir, DOWNLOAD_PART + blockId);
        if (tempFile.exists()) {
            completedSize = tempFile.length();
        } else {
            try {
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            completedSize = 0;
        }
    }

    public long getCompletedSize() {
        return completedSize;
    }

    @Override
    public void cancel() {
        isCanceled = true;
        connection.cancel();
    }

}
