package com.huxq17.download.core.task;


import android.text.TextUtils;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;


public class SimpleDownloadTask extends Task {
    private final DownloadDetailsInfo downloadInfo;
    private final DownloadConnection connection;
    private final boolean shouldUseCacheRequest;

    public SimpleDownloadTask(DownloadRequest downloadRequest) {
        connection = PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(downloadRequest.getHttpRequestBuilder());
        downloadInfo = downloadRequest.getDownloadInfo();
        shouldUseCacheRequest = downloadInfo.isFinished() && !downloadRequest.isForceReDownload();
    }

    @Override
    public void execute() {
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        File downloadFile = new File(downloadInfo.getFilePath());
        if (shouldUseCacheRequest) {
            addCacheHeader();
        }
        try {
            connection.connect();
            setCacheBean();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {
                downloadInfo.setCompletedSize(downloadInfo.getContentLength());
                downloadInfo.setFinished(1);
                downloadInfo.setProgress(100);
                downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                return;
            }
            downloadInfo.deleteDownloadFile();
            downloadInfo.setFinished(0);
            if (connection.isSuccessful() && downloadFile.createNewFile()) {
                long contentLength = Util.parseContentLength(connection.getHeader("Content-Length"));
                if (contentLength > 0) {
                    downloadInfo.setContentLength(contentLength);
                }
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
            }else{
                downloadInfo.setErrorCode(ErrorCode.ERROR_CREATE_FILE_FAILED);
            }
        } catch (IOException e) {
            e.printStackTrace();
            downloadInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            LogUtil.e("download "+downloadInfo.getUrl()+" failed,  cause by "+e.getMessage());
        } finally {
            connection.close();
        }
    }

    private void addCacheHeader() {
        DownloadProvider.CacheBean cacheBean = DBService.getInstance().queryCache(downloadInfo.getId());
        if (cacheBean != null) {
            String eTag = cacheBean.eTag;
            String lastModified = cacheBean.lastModified;
            if (!TextUtils.isEmpty(lastModified)) {
                connection.addHeader("If-Modified-Since", lastModified);
            }
            if (!TextUtils.isEmpty(eTag)) {
                connection.addHeader("If-None-Match", eTag);
            }
        }
    }

    private void setCacheBean() {
        final String lastModified = connection.getHeader("Last-Modified");
        final String eTag = connection.getHeader("ETag");
        if (!TextUtils.isEmpty(lastModified) || !TextUtils.isEmpty(eTag)) {
            downloadInfo.setCacheBean(new DownloadProvider.CacheBean(downloadInfo.getId(), lastModified, eTag));
        }
    }

    @Override
    public void cancel() {
        if (!connection.isCanceled()) {
            connection.cancel();
        }
    }

}
