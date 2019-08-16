package com.huxq17.download.action;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.db.DBService;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.provider.Provider;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class CheckCacheAction implements Action {
    private OkHttpClient okHttpClient = OKHttpUtils.get();
    private DownloadTask downloadTask;
    private DownloadRequest downloadRequest;

    private Request buildRequest(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        Provider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
        Request.Builder builder = new Request.Builder()
                .head()
                .addHeader("Accept-Encoding", "identity")
                .url(url);

        if (cacheBean != null) {
            String eTag = cacheBean.eTag;
            String lastModified = cacheBean.lastModified;
            if (!TextUtils.isEmpty(lastModified)) {
                builder.addHeader("If-Modified-Since", lastModified);
            }
            if (!TextUtils.isEmpty(eTag)) {
                builder.addHeader("If-None-Match", eTag);
            }
        }
        return builder.build();
    }

    private boolean executeRequest(Request request) {
        boolean result = true;
        DownloadDetailsInfo detailsInfo = downloadTask.getDownloadInfo();
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
            Headers headers = response.headers();
//            for (String name : headers.names()) {
//                String value = headers.get(name);
//                LogUtil.e("header name=" + name + ";value=" + value + ";responseCode=" + response.networkResponse().code());
//            }
            String lastModified = headers.get("Last-Modified");
            String eTag = headers.get("ETag");
            int responseCode = response.code();
            if (response.isSuccessful()) {
                long contentLength = getContentLength(headers);
                if (contentLength > 0) {
                    long downloadDirUsableSpace = Util.getUsableSpace(new File(downloadRequest.getFilePath()));
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
                        downloadRequest.setCacheBean(new Provider.CacheBean(downloadRequest.getId(), lastModified, eTag));
                    }
                } else {
                    detailsInfo.setErrorCode(ErrorCode.CONTENT_LENGTH_NOT_FOUND);
                    result = false;
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                if (detailsInfo.isFinished() && !downloadRequest.isForceReDownload()) {
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
        } catch (IOException e) {
            e.printStackTrace();
            detailsInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            result = false;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            detailsInfo.setErrorCode(ErrorCode.CONTENT_LENGTH_NOT_FOUND);
            result = false;
        }
        return result;
    }

    private long getContentLength(Headers headers) {
        try {
            String contentLength = headers.get("Content-Length");
//            LogUtil.e("headers="+ headers.toString());
            return Long.parseLong(contentLength);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public boolean proceed(DownloadChain chain) {
        downloadTask = chain.getDownloadTask();
        downloadRequest = downloadTask.getRequest();
        Request request = buildRequest(downloadRequest);
        return executeRequest(request);
    }
}
