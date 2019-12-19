package com.huxq17.download.core.interceptor;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.Formatter;

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
import com.huxq17.download.provider.Provider;
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
    private int responseCode;
    private String lastModified;
    private String eTag;
    private long contentLength;
    private String contentLengthField;

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        downloadRequest = chain.request();
        downloadDetailsInfo = downloadRequest.getDownloadInfo();
        DownloadConnection connection = buildRequest(downloadRequest);
        String transferEncoding;
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
                downloadDetailsInfo.setSupportBreakpoint(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            downloadDetailsInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            return downloadDetailsInfo.snapshot();
        } finally {
            connection.close();
        }

        if (contentLength == -1 &&
                isNeedHeadContentLength(transferEncoding)) {
            contentLength = headContentLength();
        }
        if (contentLength == -1 && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED) {
            downloadDetailsInfo.setSupportBreakpoint(false);
        }

        downloadDetailsInfo.setMd5(connection.getHeader("Content-MD5"));
        if (responseCode >= 200 && responseCode < 300) {
            if (contentLength > 0) {
                long downloadDirUsableSpace = Util.getUsableSpace(new File(downloadDetailsInfo.getFilePath()));
                long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
                long minUsableStorageSpace = PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
                if (downloadDirUsableSpace < contentLength * 2 || dataFileUsableSpace <= minUsableStorageSpace) {
                    downloadDetailsInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
                    Context context = PumpFactory.getService(IDownloadManager.class).getContext();
                    String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
                    LogUtil.e("Download directory usable space is " + downloadFileAvailableSize + ";but download file's contentLength is " + contentLength);
                    return downloadDetailsInfo.snapshot();
                } else {
                    downloadDetailsInfo.setContentLength(contentLength);
                    downloadDetailsInfo.setCacheBean(new Provider.CacheBean(downloadRequest.getId(), lastModified, eTag));
                }
            } else {
                //Not found content-length in http head.
                downloadDetailsInfo.setContentLength(contentLength);
            }
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
        checkDownloadFile();
        return chain.proceed(downloadRequest);
    }

    private void checkDownloadFile() {
        long contentLength = downloadDetailsInfo.getContentLength();

        File tempDir = downloadDetailsInfo.getTempDir();
        long localLength = DBService.getInstance().queryLocalLength(downloadDetailsInfo);
        if (contentLength <= 0 || contentLength != localLength) {
            //If file's length have changed,we need to re-download it.
            FileUtil.deleteDir(tempDir);
        }
        downloadDetailsInfo.setFinished(0);
        String[] childList = tempDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOWNLOAD_PART);
            }
        });
        if (childList != null && (childList.length != downloadRequest.getThreadNum() || contentLength <= 0)) {
            FileUtil.deleteDir(tempDir);
        }
        FileUtil.deleteFile(downloadDetailsInfo.getDownloadFile());

    }

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

    private DownloadConnection createConnection(String url) {
        return PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(url);
    }
}
