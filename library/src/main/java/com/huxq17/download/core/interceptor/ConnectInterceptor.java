package com.huxq17.download.core.interceptor;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Formatter;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.TaskManager;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.PumpFile;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.core.task.DownloadBlockTask;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.core.task.Task;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Response;

import static com.huxq17.download.ErrorCode.ERROR_CONTENT_LENGTH_NOT_FOUND;
import static com.huxq17.download.utils.Util.CONTENT_LENGTH_NOT_FOUND;
import static com.huxq17.download.utils.Util.DOWNLOAD_PART;

public class ConnectInterceptor implements DownloadInterceptor {
    private DownloadDetailsInfo downloadInfo;
    private DownloadTask downloadTask;
    private DownloadBlockTask firstBlockTask = null;
    private final List<DownloadBlockTask> blockList = new ArrayList<>();
    private boolean isConditionRequest;
    private DownloadConnection connection;

    private void deleteTempIfThreadNumChanged(DownloadDetailsInfo downloadInfo) {
        File tempDir = downloadInfo.getTempDir();
        if (tempDir != null) {
            String[] childList = tempDir.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(DOWNLOAD_PART);
                }
            });
            if (childList != null && childList.length != downloadInfo.getThreadNum()) {
                downloadInfo.deleteTempDir();
            }
        }
    }

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        isConditionRequest = false;
        DownloadRequest downloadRequest = chain.request();
        downloadInfo = downloadRequest.getDownloadInfo();
        downloadTask = downloadInfo.getDownloadTask();

        deleteTempIfThreadNumChanged(downloadInfo);
        DownloadConnection conn = buildRequest(downloadRequest);
        int responseCode;
        Response response = connect(conn);
        if (response == null) {
            conn.close();
            connection = null;
            if (!isCancelled()) {
                downloadInfo.setErrorCode(ErrorCode.ERROR_NETWORK_UNAVAILABLE);
            }
            return downloadInfo.snapshot();
        }
        if(!prepareDownloadFile(downloadTask, response)){
            downloadInfo.setErrorCode(ErrorCode.ERROR_CREATE_FILE_FAILED);
            return downloadInfo.snapshot();
        }
        final String lastModified = conn.getHeader("Last-Modified");
        final String eTag = conn.getHeader("ETag");
        final String acceptRanges = conn.getHeader("Accept-Ranges");
        downloadInfo.setMD5(conn.getHeader("Content-MD5"));
        downloadInfo.setTransferEncoding(conn.getHeader("Transfer-Encoding"));

        responseCode = response.code();
        long contentLength = getContentLength(conn);
        if (response.isSuccessful()) {
            if (contentLength == CONTENT_LENGTH_NOT_FOUND && !downloadInfo.isChunked()) {
                downloadInfo.setErrorCode(ERROR_CONTENT_LENGTH_NOT_FOUND);
                return closeConnectionAndReturn();
            }
            if (checkIsSpaceNotEnough(contentLength)) {
                downloadInfo.setErrorCode(ErrorCode.ERROR_USABLE_SPACE_NOT_ENOUGH);
                return closeConnectionAndReturn();
            }
        } else if (responseCode == HttpURLConnection.HTTP_NOT_MODIFIED) {
            if (downloadInfo.isFinished()) {
                downloadInfo.setCompletedSize(downloadInfo.getContentLength());
                downloadInfo.setProgress(100);
                downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                downloadTask.updateInfo();
                return closeConnectionAndReturn();
            }
        } else {
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                downloadInfo.setErrorCode(ErrorCode.ERROR_FILE_NOT_FOUND);
            } else {
                downloadInfo.setErrorCode(ErrorCode.ERROR_UNKNOWN_SERVER_ERROR);
            }
            return closeConnectionAndReturn();
        }
        if (responseCode == HttpURLConnection.HTTP_OK) {
            firstBlockTask.clearTemp();
        } else if (responseCode == HttpURLConnection.HTTP_PARTIAL) {

        }
        DownloadProvider.CacheBean cacheBean = null;
        if (!TextUtils.isEmpty(lastModified) || !TextUtils.isEmpty(eTag)) {
            cacheBean = new DownloadProvider.CacheBean(downloadRequest.getId(), lastModified, eTag);
            downloadInfo.setCacheBean(cacheBean);
        }
        boolean isServerSupportBreakPointDownload = !downloadInfo.isChunked() && cacheBean != null && (isConditionRequest || "bytes".equals(acceptRanges));
        boolean isSupportBreakPointDownload = isServerSupportBreakPointDownload && !downloadInfo.isDisableBreakPointDownload();
        if (isServerSupportBreakPointDownload) {
            DBService.getInstance().updateCache(cacheBean);
        }
        int threadNum = isSupportBreakPointDownload ? downloadRequest.getThreadNum() : 1;
        downloadInfo.setThreadNum(threadNum);
        checkDownloadFile(contentLength, isSupportBreakPointDownload);

        long completedSize = 0L;
        synchronized (blockList) {
            for (int i = 0; i < threadNum; i++) {
                if (i == 0) {
                    completedSize += firstBlockTask.getCompletedSize();
                } else {
                    DownloadBlockTask task = new DownloadBlockTask(downloadRequest, i);
                    completedSize += task.getCompletedSize();
                    blockList.add(task);
                    TaskManager.execute(task);
                }
            }
        }
        downloadInfo.setCompletedSize(completedSize);
        firstBlockTask.run();
        for (DownloadBlockTask task : blockList) {
            task.waitUntilFinished();
        }
        clearBlockList();
        return chain.proceed(downloadRequest);
    }

    public void cancel() {
        if (connection != null) {
            connection.cancel();
        }
        synchronized (blockList) {
            for (Task task : blockList) {
                task.cancel();
            }
        }
    }

    private void clearBlockList() {
        synchronized (blockList) {
            blockList.clear();
        }
    }

    private boolean checkIsSpaceNotEnough(long contentLength) {
        Context context = PumpFactory.getService(IDownloadManager.class).getContext();
        long downloadDirUsableSpace = Util.getUsableSpace(downloadInfo.getTempDir());
        long dataFileUsableSpace = Util.getUsableSpace(context.getFilesDir().getParentFile());
        long minUsableStorageSpace = PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
        if (downloadDirUsableSpace < contentLength * 2 || dataFileUsableSpace <= minUsableStorageSpace) {
            String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
            LogUtil.e("Download temp directory is" + downloadInfo.getTempDir() + " and usable space is " +
                    downloadFileAvailableSize + ";but download file's contentLength is " + contentLength);
            return true;
        }
        return false;
    }

    private void checkDownloadFile(long contentLength, boolean isSupportBreakPointDownload) {
        if (!isSupportBreakPointDownload
                || contentLength != downloadInfo.getContentLength()) {
            downloadInfo.deleteTempDir();
        }
        downloadInfo.setContentLength(contentLength);
        downloadInfo.setFinished(0);
        downloadTask.updateInfo();
    }

    private DownloadConnection buildRequest(DownloadRequest downloadRequest) {
        String id = downloadRequest.getId();
        DownloadConnection connection = createConnection(downloadRequest);
        this.connection = connection;
        firstBlockTask = new DownloadBlockTask(downloadRequest, 0, connection);
        long completedSize = firstBlockTask.getCompletedSize();
        DownloadProvider.CacheBean cacheBean = DBService.getInstance().queryCache(id);
        if (cacheBean == null) {
            return connection;
        }
        String eTag = cacheBean.eTag;
        String lastModified = cacheBean.lastModified;
        if (completedSize > 0 && !downloadInfo.isDisableBreakPointDownload()) {
            connection.addHeader("If-Range", cacheBean.getIfRangeField());
            connection.addHeader("Range", "bytes=" + completedSize + "-");
            isConditionRequest = true;
        } else if (downloadRequest.getDownloadInfo().isFinished() && !downloadRequest.isForceReDownload()) {
            if (!TextUtils.isEmpty(lastModified)) {
                connection.addHeader("If-Modified-Since", cacheBean.lastModified);
            }
            if (!TextUtils.isEmpty(eTag)) {
                connection.addHeader("If-None-Match", cacheBean.eTag);
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
        if (!downloadInfo.isChunked() && contentLength == CONTENT_LENGTH_NOT_FOUND) {
            contentLength = Util.parseContentLength(connection.getHeader("Content-Length"));
        }
        return contentLength;
    }

    private Response connect(DownloadConnection connection) {
        Response response = null;
        if (!isCancelled()) {
            try {
                response = connection.connect();
            } catch (IOException e) {
                if (!Thread.currentThread().isInterrupted()) {
                    e.printStackTrace();
                }
                connection.close();
            }
        }
        return response;
    }

    private boolean isCancelled() {
        return Thread.currentThread().isInterrupted();
    }

    private DownloadInfo closeConnectionAndReturn() {
        connection.close();
        connection = null;
        return downloadInfo.snapshot();
    }

    private DownloadConnection createConnection(DownloadRequest downloadRequest) {
        return PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory()
                .create(downloadRequest.getHttpRequestBuilder());
    }

    private boolean prepareDownloadFile(DownloadTask downloadTask, Response response) {
        DownloadDetailsInfo downloadDetailsInfo = downloadTask.getDownloadInfo();
        PumpFile downloadFile = downloadDetailsInfo.getDownloadFile();
        Uri schemaUri = downloadDetailsInfo.getSchemaUri();
        String cachePath = Util.getPumpCachePath(DownloadProvider.context);
        boolean shouldUseInternalStorageAboveQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                schemaUri == null && (downloadFile == null || !downloadFile.getPath().contains(cachePath));
        //set download path if download path is null or name is absent.
        if (downloadFile == null || downloadFile.isDirectory() || shouldUseInternalStorageAboveQ) {
            String parentDirectory = shouldUseInternalStorageAboveQ ? cachePath :
                    downloadFile != null ? downloadFile.getPath() : cachePath;
            String fileName;
            if (downloadFile == null || downloadFile.isDirectory()) {
                String contentDisposition = response.header("Content-Disposition");
                String contentType = response.header("Content-Type");
                fileName = Util.guessFileName(response.request().url().toString(), contentDisposition, contentType);
            } else {
                fileName = downloadFile.getName();
            }
            downloadDetailsInfo.setFilePath(parentDirectory + File.separatorChar + fileName);
        }
        downloadDetailsInfo.deleteDownloadFile();
        boolean createResult = downloadDetailsInfo.getDownloadFile().createNewFile();
        downloadTask.updateInfo();
        return createResult;
    }
}
