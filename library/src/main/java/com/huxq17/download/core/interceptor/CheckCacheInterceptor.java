package com.huxq17.download.core.interceptor;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.db.DBService;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;

import static com.huxq17.download.utils.Util.DOWNLOAD_PART;

public class CheckCacheInterceptor implements DownloadInterceptor {
    private DownloadDetailsInfo downloadDetailsInfo;
    private DownloadRequest downloadRequest;
    private String lastModified;
    private String eTag;
    private static final int CONTENT_LENGTH_NOT_FOUND = -1;

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        downloadRequest = chain.request();
        downloadDetailsInfo = downloadRequest.getDownloadInfo();
        DownloadConnection connection = buildRequest(downloadRequest);
        int responseCode;
        long contentLength;
        try {
            connection.connect();
            String transferEncoding = connection.getHeader("Transfer-Encoding");
            lastModified = connection.getHeader("Last-Modified");
            setFilePathIfNeed(connection);
            eTag = connection.getHeader("ETag");
            String contentLengthField = connection.getHeader("Content-Length");
            downloadDetailsInfo.setMD5(connection.getHeader("Content-MD5"));
            responseCode = connection.getResponseCode();
            contentLength = getContentLength(connection);
            long originalContentLength = Util.parseContentLength(connection.getHeader("Content-Length"));
            if (responseCode == HttpURLConnection.HTTP_OK) {
                contentLength = originalContentLength;
                downloadDetailsInfo.setSupportBreakpoint(false);
            }
            if (contentLength == CONTENT_LENGTH_NOT_FOUND && isNeedHeadContentLength(contentLengthField, transferEncoding)) {
                contentLength = headContentLength();
            }
        } catch (IOException e) {
            e.printStackTrace();
            downloadDetailsInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            return downloadDetailsInfo.snapshot();
        } finally {
            connection.close();
        }
        return parseResponse(chain, responseCode, contentLength);
    }

    private DownloadInfo parseResponse(DownloadChain chain, int responseCode, long contentLength) {
        if (responseCode >= 200 && responseCode < 300) {
            if (contentLength != CONTENT_LENGTH_NOT_FOUND) {
                long downloadDirUsableSpace = Util.getUsableSpace(new File(downloadDetailsInfo.getFilePath()));
                long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
                long minUsableStorageSpace = PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
                if (downloadDirUsableSpace < contentLength * 2 || dataFileUsableSpace <= minUsableStorageSpace) {
                    //space is unusable.
                    downloadDetailsInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
                    Context context = PumpFactory.getService(IDownloadManager.class).getContext();
                    String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
                    LogUtil.e("Download directory usable space is " + downloadFileAvailableSize + ";but download file's contentLength is " + contentLength);
                    return downloadDetailsInfo.snapshot();
                } else {
                    downloadDetailsInfo.setContentLength(contentLength);
                    downloadDetailsInfo.setCacheBean(new DownloadProvider.CacheBean(downloadRequest.getId(), lastModified, eTag));
                }
            }
            downloadDetailsInfo.setSupportBreakpoint(contentLength != CONTENT_LENGTH_NOT_FOUND);
            File tempDir = downloadDetailsInfo.getTempDir();
            String[] childList = tempDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(DOWNLOAD_PART);
                }
            });
            if (childList != null && childList.length != downloadRequest.getThreadNum() ||
                    contentLength != downloadDetailsInfo.getContentLength()
                    || contentLength == CONTENT_LENGTH_NOT_FOUND) {
                FileUtil.deleteDir(tempDir);
            }
            downloadDetailsInfo.setContentLength(contentLength);
            downloadDetailsInfo.setFinished(0);
            FileUtil.deleteFile(downloadDetailsInfo.getDownloadFile());
        } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            if (downloadDetailsInfo.isFinished()) {
                downloadDetailsInfo.setCompletedSize(downloadDetailsInfo.getContentLength());
                downloadDetailsInfo.setFinished(1);
                return downloadDetailsInfo.snapshot();
            }
        } else {
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                downloadDetailsInfo.setErrorCode(ErrorCode.FILE_NOT_FOUND);
            } else {
                downloadDetailsInfo.setErrorCode(ErrorCode.UNKNOWN_SERVER_ERROR);
            }
            return downloadDetailsInfo.snapshot();
        }
        return chain.proceed(downloadRequest);
    }


    private void setFilePathIfNeed(DownloadConnection connection) {
        if (downloadRequest.getFilePath() == null) {
            String fileName = Util.guessFileName(downloadRequest.getUrl(),
                    connection.getHeader("Content-Disposition"), connection.getHeader("Content-Type"));
            downloadRequest.setFilePath(Util.getCachePath(DownloadProvider.context) + "/" + fileName);
            DBService.getInstance().updateInfo(downloadRequest.getDownloadInfo());
        }
    }

    private DownloadConnection buildRequest(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        DownloadConnection connection = createConnection(url);
        connection.addHeader("Range", "bytes=0-0");
        if (downloadRequest.getDownloadInfo().isFinished() && !downloadRequest.isForceReDownload()) {
            DownloadProvider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
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

    private long getContentLength(DownloadConnection connection) {
        long contentLength = CONTENT_LENGTH_NOT_FOUND;
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

    private boolean isNeedHeadContentLength(String contentLengthField, String transferEncoding) {
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
        return CONTENT_LENGTH_NOT_FOUND;
    }

    private DownloadConnection createConnection(String url) {
        return PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(url);
    }
}
