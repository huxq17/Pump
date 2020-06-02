package com.huxq17.download.core.task;


import android.text.TextUtils;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.utils.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.Response;

import static com.huxq17.download.utils.Util.DOWNLOAD_PART;


public class DownloadBlockTask extends Task {
    private DownloadConnection connection;
    private int blockId;
    private long completedSize;
    private File tempFile;
    private DownloadDetailsInfo downloadInfo;
    private boolean isConnected;

    public DownloadBlockTask(DownloadRequest downloadRequest, int blockId) {
        this(downloadRequest, blockId, null);
    }

    public DownloadBlockTask(DownloadRequest downloadRequest, int blockId, DownloadConnection connection) {
        downloadInfo = downloadRequest.getDownloadInfo();
        isConnected = connection != null;
        if (connection == null) {
            this.connection = PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory().create(downloadRequest.getHttpRequestBuilder());
        } else {
            this.connection = connection;
        }
        this.blockId = blockId;
        calculateCompletedSize();
    }

    @Override
    public void cancel() {
        if (!connection.isCanceled()) {
            connection.cancel();
        }
    }

    @Override
    public void execute() {
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        long threadNum = downloadInfo.getThreadNum();
        long fileLength = downloadInfo.getContentLength();
        long startPosition = blockId * fileLength / threadNum + completedSize;
        long endPosition;
        if (threadNum == blockId + 1) {
            endPosition = fileLength - 1;
        } else {
            endPosition = (blockId + 1) * fileLength / threadNum - 1;
        }

        if (startPosition != endPosition + 1) {
            try {
                if (!isConnected) {
                    DownloadProvider.CacheBean cacheBean = downloadInfo.getCacheBean();
                    String eTag = cacheBean.eTag;
                    String lastModified = cacheBean.lastModified;
                    connection.addHeader("Range", "bytes=" + startPosition + "-");
                    if (!TextUtils.isEmpty(lastModified)) {
                        connection.addHeader("If-Unmodified-Since", lastModified);
                    }
                    if (!TextUtils.isEmpty(eTag)) {
                        connection.addHeader("If-Match", eTag);
                    }
                    Response response = connection.connect();
                    int code = response.code();
                    if (code == HttpURLConnection.HTTP_PARTIAL) {
                        download(connection, downloadTask, startPosition, endPosition + 1);
                    } else if (code == HttpURLConnection.HTTP_PRECON_FAILED) {
                        downloadInfo.setErrorCode(ErrorCode.ERROR_NETWORK_UNAVAILABLE);
                        downloadTask.cancel();
                        clearTemp();
                    } else {
                        downloadInfo.setErrorCode(ErrorCode.ERROR_NETWORK_UNAVAILABLE);
                        downloadTask.cancel();
                    }
                } else {
                    download(connection, downloadTask, startPosition, endPosition + 1);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                downloadInfo.setErrorCode(ErrorCode.ERROR_NETWORK_UNAVAILABLE);
            } finally {
                connection.close();
            }
        }
    }

    private void download(DownloadConnection connection, DownloadTask downloadTask,
                          long startPosition, long endPosition) throws IOException {
        int len;
        createTempFileIfNeed();
        connection.prepareDownload(tempFile);
        byte[] buffer = new byte[8092];
        int byteCount = Math.min(buffer.length, (int) (endPosition - startPosition));
        while (!isCanceled() && startPosition < endPosition
                && (len = connection.downloadBuffer(buffer, 0, byteCount)) != -1) {
            startPosition += len;
            completedSize +=len;
            long remainCount = endPosition - startPosition;
            if (remainCount < byteCount) {
                byteCount = (int) remainCount;
            }
            if (!downloadTask.onDownload(len)) {
                break;
            }
        }
        connection.flushDownload();
    }

    private void calculateCompletedSize() {
        File tempDir = downloadInfo.getTempDir();
        if (tempDir != null) {
            tempFile = new File(tempDir, DOWNLOAD_PART + blockId);
            completedSize = tempFile.length();
        }
    }

    private void createTempFileIfNeed() {
        if (tempFile != null && tempFile.exists()) return;

        File tempDir = downloadInfo.getTempDir();
        tempFile = new File(tempDir, DOWNLOAD_PART + blockId);
        try {
            if (!tempDir.exists()) {
                tempDir.mkdirs();
            }
            tempFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getCompletedSize() {
        return completedSize;
    }

    public void clearTemp() {
        FileUtil.deleteFile(tempFile);
        completedSize = 0;
    }

}
