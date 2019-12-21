package com.huxq17.download.core.task;


import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.IOException;


public class SimpleDownloadTask extends Task {
    private DownloadDetailsInfo downloadInfo;
    private DownloadConnection connection;

    public SimpleDownloadTask(DownloadRequest downloadRequest) {
        downloadInfo = downloadRequest.getDownloadInfo();
        connection = PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(downloadRequest.getUrl());
    }

    @Override
    public void execute() {
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        File downloadFile = new File(downloadInfo.getFilePath());
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
                    while (!isCanceled() && (len = connection.downloadBuffer(buffer)) != -1) {
                        if (!downloadTask.onDownload(len)) {
                            break;
                        }
                    }
                    connection.flushDownload();
                    downloadInfo.setContentLength(downloadFile.length());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            downloadInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
        } finally {
            connection.close();
        }
    }

    @Override
    public void cancel() {
        if (!connection.isCanceled()) {
            connection.cancel();
        }
    }

}
