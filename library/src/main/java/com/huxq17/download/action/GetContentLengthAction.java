package com.huxq17.download.action;

import android.text.TextUtils;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.db.DBService;
import com.huxq17.download.provider.Provider;
import com.huxq17.download.task.DownloadTask;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetContentLengthAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        boolean result = true;
        DownloadTask downloadTask = chain.getDownloadTask();
        DownloadRequest downloadRequest = downloadTask.getRequest();
        DownloadDetailsInfo detailsInfo = downloadTask.getDownloadInfo();
        OkHttpClient okHttpClient = OKHttpUtils.get();
        String url = detailsInfo.getUrl();
        Provider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
        Request.Builder builder = new Request.Builder()
                .get()
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
        Request request = builder.build();
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
            long contentLength = response.body().contentLength();
            if (response.cacheResponse() != null) {
                detailsInfo.setContentLength(contentLength);
                downloadRequest.setCacheBean(new Provider.CacheBean(url, lastModified, eTag));
            } else {
                int responseCode = response.code();
                if (response.isSuccessful()) {
                    detailsInfo.setContentLength(contentLength);
                    downloadRequest.setCacheBean(new Provider.CacheBean(url, lastModified, eTag));
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
            }
        } catch (IOException e) {
            e.printStackTrace();
            detailsInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            result = false;
        }
        return result;
    }
}
