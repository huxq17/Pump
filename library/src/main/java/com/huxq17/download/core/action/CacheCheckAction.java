package com.huxq17.download.core.action;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.db.DBService;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.provider.Provider;
import com.huxq17.download.core.task.DownloadTask;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;


public class CacheCheckAction implements Action {
    private DownloadTask downloadTask;
    private DownloadRequest downloadRequest;

    private DownloadConnection buildRequest(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        DownloadConnection connection = createConnection(url);
        connection.addHeader("Range", "bytes=0-0");
        if (downloadRequest.getDownloadInfo().isFinished() && !downloadRequest.isForceReDownload()) {
            Provider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
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
        return connection;
    }

    private int responseCode;
    private String lastModified;
    private String eTag;
    private long contentLength;
    private String contentLengthField;

    private boolean parseResponse(DownloadConnection connection) {
        boolean result = true;
        String transferEncoding = null;
        downloadTask.setSupportBreakpoint(true);
        try {
            connection.connect();
            transferEncoding = connection.getHeader("Transfer-Encoding");
            lastModified = connection.getHeader("Last-Modified");
            if (downloadRequest.getFilePath() == null) {
                String fileName = Util.guessFileName(downloadRequest.getUrl(), connection.getHeader("Content-Disposition"), connection.getHeader("Content-Type"));
                downloadRequest.setFilePath(Util.getCachePath(PumpFactory.getService(IDownloadManager.class).getContext()) + "/" + fileName);
                DBService.getInstance().updateInfo(downloadRequest.getDownloadInfo());
            }
            eTag = connection.getHeader("ETag");
            contentLengthField = connection.getHeader("Content-Length");
            responseCode = connection.getResponseCode();
            contentLength = getContentLength(connection);
            long originalContentLength = Util.parseContentLength(connection.getHeader("Content-Length"));
            if (responseCode == HttpURLConnection.HTTP_OK) {
                contentLength = originalContentLength;
                downloadTask.setSupportBreakpoint(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        } finally {
            connection.close();
        }
        if (contentLength == -1 &&
                isNeedHeadContentLength(transferEncoding)) {
            contentLength = headContentLength();
        }
        if (contentLength == -1 && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
            downloadTask.setSupportBreakpoint(false);
        }
        return result;
    }

    private boolean executeRequest(DownloadConnection connection) {
        boolean result = true;
        DownloadDetailsInfo detailsInfo = downloadTask.getDownloadInfo();
        if (parseResponse(connection)) {
            detailsInfo.setMd5(connection.getHeader("Content-MD5"));
            if (responseCode >= 200 && responseCode < 300) {
                if (contentLength > 0) {
                    long downloadDirUsableSpace = Util.getUsableSpace(new File(detailsInfo.getFilePath()));
                    long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
                    long minUsableStorageSpace = PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
                    if (downloadDirUsableSpace < contentLength * 2 || dataFileUsableSpace <= minUsableStorageSpace) {
                        detailsInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
                        result = false;

                        Context context = PumpFactory.getService(IDownloadManager.class).getContext();
                        String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
                        LogUtil.e("Download directory usable space is " + downloadFileAvailableSize + ";but download file's contentLength is " + contentLength);
                    } else {
                        detailsInfo.setContentLength(contentLength);
                        detailsInfo.setCacheBean(new Provider.CacheBean(downloadRequest.getId(), lastModified, eTag));
                    }
                } else {
                    //Not found content-length in http head.
                    detailsInfo.setContentLength(contentLength);
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                if (detailsInfo.isFinished()) {
                    detailsInfo.setCompletedSize(detailsInfo.getContentLength());
                    detailsInfo.setFinished(1);
                    result = false;
                }
            } else {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    detailsInfo.setErrorCode(ErrorCode.FILE_NOT_FOUND);
                } else {
                    detailsInfo.setErrorCode(ErrorCode.UNKNOWN_SERVER_ERROR);
                }
                result = false;
            }
        } else {
            detailsInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            result = false;
        }
        return result;
    }

    private long getContentLength(DownloadConnection connection) {
        long contentLength = -1;
        String contentRange = connection.getHeader("Content-Range");
        if (contentRange != null) {
            final String[] session = contentRange.split("/");
            if (session.length >= 2) {
                try {
                    contentLength = Long.parseLong(session[1]);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return contentLength;
    }

    private boolean isNeedHeadContentLength(String transferEncoding) {
        if (transferEncoding != null && transferEncoding.equals("chunked")) {
            // because of the Transfer-Encoding can certain the result is right, so pass.
            return false;
        }

        if (contentLengthField == null || contentLengthField.length() <= 0) {
            // because of the response header isn't contain the Content-Length so the HEAD method
            // request is useless, because we plan to get the right instance-length on the
            // Content-Length field through the response header of non 0-0 Range HEAD method request
            return false;
        }
        // because of the response header contain Content-Length, but because of we using Range: 0-0
        // so we the Content-Length is always 1 now, we can't use it, so we try to use HEAD method
        // request just for get the certain instance-length.
        return true;
    }

    private long headContentLength() {
        DownloadConnection connection = createConnection(downloadRequest.getUrl());
        try {
            connection.connect();
            return Util.parseContentLength(connection.getHeader("Content-Length"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.close();
        }
        return -1;
    }

    @Override
    public boolean proceed(DownloadChain chain) {
        downloadTask = chain.getDownloadTask();
        downloadRequest = downloadTask.getRequest();
        return executeRequest(buildRequest(downloadRequest));
    }

    private DownloadConnection createConnection(String url) {
        return PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(url);
    }
}
