package com.huxq17.download.task;


import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.connection.DownloadConnection;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


public class SimpleDownloadTask implements Task {
    private DownloadChain downloadChain;
    private boolean isCanceled;
    private DownloadRequest downloadRequest;
    private DownloadConnection connection;

    public SimpleDownloadTask(DownloadChain downloadChain) {
        this.downloadChain = downloadChain;
        downloadRequest = downloadChain.getDownloadTask().getRequest();
        connection = PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(downloadRequest.getUrl());
    }

    @Override
    public void run() {
        isCanceled = false;
        DownloadTask downloadTask = downloadChain.getDownloadTask();
        DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
        File downloadFile = new File(downloadRequest.getFilePath());
        Util.deleteFile(downloadFile);

        try {
            if (downloadFile.createNewFile()) {
                connection.connect();

                long contentLength = Util.parseContentLength(connection.getHeader("Content-Length"));
                if (contentLength > 0) {
                    downloadInfo.setContentLength(contentLength);
                }
                if (connection.isSuccessful()) {
                    byte[] buffer = new byte[8092];
                    connection.prepareDownload(downloadFile);

                    int len;
                    while (!isCanceled && (len = connection.downloadBuffer(buffer)) != -1) {
                        if (!downloadTask.onDownload(len)) {
                            break;
                        }
                    }
                    connection.downloadFlush();
                    downloadInfo.setContentLength(downloadFile.length());
                }
            }
        } catch (IOException e) {
            if (!connection.isCanceled()) {
                e.printStackTrace();
                downloadTask.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public void cancel() {
        isCanceled = true;
        connection.cancel();
    }
}
