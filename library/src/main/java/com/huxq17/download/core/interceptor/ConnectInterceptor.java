package com.huxq17.download.core.interceptor;

import android.content.Context;
import android.os.Environment;
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
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.core.task.DownloadBlockTask;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.core.task.Task;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
    private DownloadBlockTask firstBlockTask = null;
    private final List<Task> blockList = new ArrayList<>();

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        downloadRequest = chain.request();
        downloadDetailsInfo = downloadRequest.getDownloadInfo();
        downloadTask = downloadDetailsInfo.getDownloadTask();

        DownloadConnection connection = buildRequest(downloadRequest);
        int responseCode;
        Response response;
        try {
            response = connection.connect();

            lastModified = connection.getHeader("Last-Modified");
            setFilePathIfNeed(downloadTask, response);
            eTag = connection.getHeader("ETag");
            String acceptRanges = connection.getHeader("Accept-Ranges");
            downloadDetailsInfo.setMD5(connection.getHeader("Content-MD5"));

            responseCode = response.code();
            long contentLength = getContentLength(connection);
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
            DownloadProvider.CacheBean cacheBean = null;
            if (!TextUtils.isEmpty(lastModified) || !TextUtils.isEmpty(eTag)) {
                cacheBean = new DownloadProvider.CacheBean(downloadRequest.getId(), lastModified, eTag);
                downloadDetailsInfo.setCacheBean(cacheBean);
                DBService.getInstance().updateCache(cacheBean);
            }


            if (responseCode == HttpURLConnection.HTTP_OK) {
                firstBlockTask.clearTemp();
            }else if(responseCode == HttpURLConnection.HTTP_PARTIAL){

            }
            checkDownloadFile(contentLength);

            int threadNum = cacheBean != null && "bytes".equals(acceptRanges) ? downloadRequest.getThreadNum() : 1;
            downloadDetailsInfo.setThreadNum(threadNum);
            List<Future> futures = new ArrayList<>(threadNum);
            long completedSize = 0L;
            synchronized (blockList) {
                for (int i = 0; i < threadNum; i++) {
                    if (i == 0) {
                        completedSize += firstBlockTask.getCompletedSize();
                    } else {
                        DownloadBlockTask task = new DownloadBlockTask(downloadRequest, i);
                        completedSize += task.getCompletedSize();
                        futures.add(TaskManager.submit(task));
                    }

                }
            }
            downloadDetailsInfo.setCompletedSize(completedSize);
            firstBlockTask.run();
            try {
                for (Future future : futures) {
                    if (!future.isDone()) {
                        future.get();
                    }
                }
            } catch (ExecutionException | InterruptedException | CancellationException e) {
                cancel();
            }
            clearBlockList();
        } catch (IOException e) {
            e.printStackTrace();
            downloadDetailsInfo.setErrorCode(ErrorCode.ERROR_NETWORK_UNAVAILABLE);
            return downloadDetailsInfo.snapshot();
        } finally {
            connection.close();
        }
        return chain.proceed(downloadRequest);
    }

    public void cancel() {
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

    private void checkDownloadFile(long contentLength) {
        File tempDir = downloadDetailsInfo.getTempDir();
        String[] childList = tempDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOWNLOAD_PART);
            }
        });
        if (childList != null && childList.length != downloadDetailsInfo.getThreadNum()
                || contentLength != downloadDetailsInfo.getContentLength()
                || downloadRequest.isDisableBreakPointDownload()) {
            downloadDetailsInfo.deleteTempDir();
        }
        downloadDetailsInfo.setContentLength(contentLength);
        downloadDetailsInfo.setFinished(0);
        downloadDetailsInfo.deleteDownloadFile();
        downloadTask.updateInfo();
    }

    private DownloadConnection buildRequest(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        DownloadConnection connection = createConnection(downloadRequest);
        firstBlockTask = new DownloadBlockTask(downloadRequest, 0, connection);
        long completedSize = firstBlockTask.getCompletedSize();
        DownloadProvider.CacheBean cacheBean = DBService.getInstance().queryCache(url);
        if (cacheBean == null) {
            return connection;
        }
        String eTag = cacheBean.eTag;
        String lastModified = cacheBean.lastModified;
        if (completedSize > 0) {
            connection.addHeader("If-Range", cacheBean.getIfRangeField());
            connection.addHeader("Range", "bytes=" + completedSize + "-");
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
        if(contentLength==CONTENT_LENGTH_NOT_FOUND){
            contentLength = Util.parseContentLength(connection.getHeader("Content-Length"));
        }
        return contentLength;
    }


    private DownloadConnection createConnection(DownloadRequest downloadRequest) {
        return PumpFactory.getService(IDownloadConfigService.class).getDownloadConnectionFactory()
                .create(downloadRequest.getHttpRequestBuilder());
    }

}
