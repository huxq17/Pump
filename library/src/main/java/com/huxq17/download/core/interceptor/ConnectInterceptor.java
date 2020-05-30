package com.huxq17.download.core.interceptor;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Response;

import static com.huxq17.download.ErrorCode.ERROR_CONTENT_LENGTH_NOT_FOUND;
import static com.huxq17.download.utils.Util.CONTENT_LENGTH_NOT_FOUND;
import static com.huxq17.download.utils.Util.DOWNLOAD_PART;
import static com.huxq17.download.utils.Util.setFilePathIfNeed;

public class ConnectInterceptor implements DownloadInterceptor {
    private DownloadDetailsInfo downloadDetailsInfo;
    private DownloadRequest downloadRequest;
    private String lastModified;
    private String eTag;
    private DownloadTask downloadTask;
    private String localETag;
    private String localLastModified;
    private File tempFile;

    private long calculateCompletedSize() {
        File tempDir = downloadDetailsInfo.getTempDir();
        tempFile = new File(tempDir, DOWNLOAD_PART + 0);
        if (tempFile.exists()) {
            return tempFile.length();
        } else {
            try {
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        downloadRequest = chain.request();
        downloadDetailsInfo = downloadRequest.getDownloadInfo();
        downloadTask = downloadDetailsInfo.getDownloadTask();

        DownloadConnection connection = buildRequest(downloadRequest);
        int responseCode;
        long contentLength;
        Response response;
        try {
            response = connection.connect();

            lastModified = connection.getHeader("Last-Modified");
            setFilePathIfNeed(downloadTask, response);
            eTag = connection.getHeader("ETag");
            String contentLengthField = connection.getHeader("Content-Length");
            downloadDetailsInfo.setMD5(connection.getHeader("Content-MD5"));

            responseCode = response.code();
            contentLength = Util.parseContentLength(contentLengthField);
            if (response.isSuccessful()) {
                if (contentLength == CONTENT_LENGTH_NOT_FOUND) {
                    downloadDetailsInfo.setErrorCode(ERROR_CONTENT_LENGTH_NOT_FOUND);
                    return downloadDetailsInfo.snapshot();
                }
                if (checkIsSpaceNotEnough(contentLength)) {
                    return downloadDetailsInfo.snapshot();
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
                if (downloadDetailsInfo.isFinished()) {
                    downloadDetailsInfo.setCompletedSize(downloadDetailsInfo.getContentLength());
                    downloadDetailsInfo.setProgress(100);
                    downloadDetailsInfo.setStatus(DownloadInfo.Status.FINISHED);
                    downloadTask.updateInfo();
                    return downloadDetailsInfo.snapshot();
                }
            } else {
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_FILE_NOT_FOUND);
                } else {
                    downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_UNKNOWN_SERVER_ERROR);
                }
                return downloadDetailsInfo.snapshot();
            }
            if (!TextUtils.isEmpty(lastModified) || !TextUtils.isEmpty(eTag)) {
                downloadDetailsInfo.setCacheBean(new DownloadProvider.CacheBean(downloadRequest.getId(), lastModified, eTag));
            }
            DBService.getInstance().updateCache(new DownloadProvider.CacheBean(downloadRequest.getId(), "", ""));
            checkDownloadFile(contentLength);
            int threadCount = downloadRequest.isDisableBreakPointDownload() ? 1 : downloadRequest.getThreadNum();
            if (threadCount == 1) {

            } else {

            }
        } catch (
                IOException e) {
            e.printStackTrace();
            downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_NETWORK_UNAVAILABLE);
            return downloadDetailsInfo.snapshot();
        } finally {
            connection.close();
        }
        return parseResponse(chain, response, contentLength);
    }

    private boolean checkIsSpaceNotEnough(long contentLength) {
        long downloadDirUsableSpace = Util.getUsableSpace(new File(downloadDetailsInfo.getFilePath()));
        long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
        long minUsableStorageSpace = PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
        if (downloadDirUsableSpace < contentLength * 2 || dataFileUsableSpace <= minUsableStorageSpace) {
            downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_USABLE_SPACE_NOT_ENOUGH);
            Context context = PumpFactory.getService(IDownloadManager.class).getContext();
            String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
            LogUtil.e("Download directory usable space is " + downloadFileAvailableSize + ";but download file's contentLength is " + contentLength);
            return true;
        }
        return false;
    }

    private DownloadInfo parseResponse(DownloadChain chain, Response response, long contentLength) {
        if (response.isSuccessful()) {
            if (contentLength != CONTENT_LENGTH_NOT_FOUND) {
                long downloadDirUsableSpace = Util.getUsableSpace(new File(downloadDetailsInfo.getFilePath()));
                long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
                long minUsableStorageSpace = PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
                if (downloadDirUsableSpace < contentLength * 2 || dataFileUsableSpace <= minUsableStorageSpace) {
                    //space is unusable.
                    downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_USABLE_SPACE_NOT_ENOUGH);
                    Context context = PumpFactory.getService(IDownloadManager.class).getContext();
                    String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
                    LogUtil.e("Download directory usable space is " + downloadFileAvailableSize + ";but download file's contentLength is " + contentLength);
                    return downloadDetailsInfo.snapshot();
                } else {
                    if (!TextUtils.isEmpty(lastModified) || !TextUtils.isEmpty(eTag)) {
                        downloadDetailsInfo.setCacheBean(new DownloadProvider.CacheBean(downloadRequest.getId(), lastModified, eTag));
                    }
                }
            }
            if (response.code() == HttpURLConnection.HTTP_PARTIAL) {
                downloadDetailsInfo.setSupportBreakpoint(contentLength != CONTENT_LENGTH_NOT_FOUND);
            } else {
                downloadDetailsInfo.setSupportBreakpoint(false);
            }
            checkDownloadFile(contentLength);
        } else if (response.code() == HttpURLConnection.HTTP_NOT_MODIFIED) {
            if (downloadDetailsInfo.isFinished()) {
                downloadDetailsInfo.setCompletedSize(downloadDetailsInfo.getContentLength());
                downloadDetailsInfo.setProgress(100);
                downloadDetailsInfo.setStatus(DownloadInfo.Status.FINISHED);
                downloadTask.updateInfo();
                return downloadDetailsInfo.snapshot();
            } else {
                checkDownloadFile(contentLength);
            }
        } else {
            if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_FILE_NOT_FOUND);
            } else {
                downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_UNKNOWN_SERVER_ERROR);
            }
            return downloadDetailsInfo.snapshot();
        }
        return chain.proceed(downloadRequest);
    }

    private void checkDownloadFile(long contentLength) {
        File tempDir = downloadDetailsInfo.getTempDir();
        String[] childList = tempDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOWNLOAD_PART);
            }
        });
        if (childList != null && childList.length != downloadRequest.getThreadNum()
                || contentLength != downloadDetailsInfo.getContentLength()
                || !localLastModified.equals(lastModified)
                || !localETag.equals(eTag)
                || downloadRequest.isDisableBreakPointDownload()) {
            downloadDetailsInfo.deleteTempDir();
        }
        downloadDetailsInfo.setContentLength(contentLength);
        downloadDetailsInfo.setFinished(0);
        downloadDetailsInfo.deleteDownloadFile();
        downloadTask.updateInfo();
    }

    private DownloadConnection buildRequest(DownloadRequest downloadRequest) {
        long completedSize = calculateCompletedSize();
        String url = downloadRequest.getUrl();
        DownloadConnection connection = createConnection(downloadRequest);
        if (downloadRequest.getDownloadInfo().isFinished() && !downloadRequest.isForceReDownload()) {
            DownloadProvider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
            if (cacheBean != null) {
                localETag = cacheBean.eTag;
                localLastModified = cacheBean.lastModified;
                if (!TextUtils.isEmpty(lastModified)) {
                    connection.addHeader("If-Modified-Since", localETag);
                }
                if (!TextUtils.isEmpty(eTag)) {
                    connection.addHeader("If-None-Match", localLastModified);
                }
            }

        }
        return connection;
    }

    private DownloadConnection createConnection(DownloadRequest downloadRequest) {
        return PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory()
                .create(downloadRequest.getHttpRequestBuilder());
    }
}
