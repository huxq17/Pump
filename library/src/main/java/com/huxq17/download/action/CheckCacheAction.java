package com.huxq17.download.action;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.Nullable;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Request.Builder builder = new Request.Builder()
                .get()
                .addHeader("Range", "bytes=0-0")
                .url(url);

        if (downloadRequest.getDownloadInfo().isFinished()) {
            Provider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
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
        }
        return builder.build();
    }

    private int responseCode;
    private String lastModified;
    private String eTag;
    private long contentLength;
    private String contentLengthField;

    private boolean parseResponse(Request request) {
        boolean result = true;
        Response response = null;
        String transferEncoding = null;
        try {
            response = okHttpClient.newCall(request).execute();
            Headers headers = response.headers();
            transferEncoding = headers.get("Transfer-Encoding");
            lastModified = headers.get("Last-Modified");
            if (downloadRequest.getFilePath() == null) {
                String fileName = Util.getFileNameByUrl(downloadRequest.getUrl(), headers.get("Content-Disposition"), headers.get("Content-Type"));
                downloadRequest.setFilePath(Util.getCachePath(PumpFactory.getService(IDownloadManager.class).getContext()) + "/" + fileName);
                DBService.getInstance().updateInfo(downloadRequest.getDownloadInfo());
            }
            eTag = headers.get("ETag");
            contentLengthField = headers.get("Content-Length");
            downloadRequest.setMd5(headers.get("Content-MD5"));
            responseCode = response.code();
            contentLength = getContentLength(headers);
            long originalContentLength = Util.parseContentLength(headers.get("Content-Length"));
            if (contentLength == -1 && responseCode != HttpURLConnection.HTTP_PARTIAL) {
            //TODO Downgrade here.
                contentLength = originalContentLength;
            }
        } catch (IOException e) {
            e.printStackTrace();
            result = false;
        } finally {
            Util.closeQuietly(response);
        }
        if (contentLength == -1 &&
                isNeedHeadContentLength(transferEncoding)) {
            contentLength = headContentLength();
            if (contentLength != -1) {
                result = true;
            }
        }
        return result;
    }

    private boolean executeRequest(Request request) {
        boolean result = true;
        DownloadDetailsInfo detailsInfo = downloadTask.getDownloadInfo();
        if (parseResponse(request)) {
            if (responseCode >= 200 && responseCode < 300) {
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
        } else {
            detailsInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            result = false;
        }
        return result;
    }

    private long getContentLength(Headers headers) {
        long contentLength = -1;
        String contentRange = headers.get("Content-Range");
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
        String url = downloadRequest.getUrl();
        Request.Builder builder = new Request.Builder()
                .head()
                .url(url);
        Response response = null;
        try {
            response = okHttpClient.newCall(builder.build()).execute();
            Headers headers = response.headers();
            return Util.parseContentLength(headers.get("Content-Length"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Util.closeQuietly(response);
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
