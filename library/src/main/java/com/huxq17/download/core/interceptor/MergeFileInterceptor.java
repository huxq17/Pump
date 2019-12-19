package com.huxq17.download.core.interceptor;

import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;

import java.io.File;
import java.io.FilenameFilter;

import static com.huxq17.download.utils.Util.DOWNLOAD_PART;

public class MergeFileInterceptor implements DownloadInterceptor {
    private DownloadDetailsInfo downloadInfo;
    private DownloadRequest downloadRequest;

    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        downloadRequest = chain.request();
        downloadInfo = downloadRequest.getDownloadInfo();
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        if (!downloadInfo.isSupportBreakpoint()) {
            return chain.proceed(downloadRequest);
        }
        synchronized (downloadTask.getLock()) {
            long fileLength = downloadInfo.getContentLength();
            File tempDir = downloadInfo.getTempDir();

            long completedSize = downloadInfo.getCompletedSize();
            File[] downloadPartFiles = tempDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(DOWNLOAD_PART);
                }
            });
            if (fileLength > 0 && completedSize == fileLength && downloadPartFiles != null && downloadPartFiles.length == downloadTask.getRequest().getThreadNum()) {
                File file = downloadInfo.getDownloadFile();
                FileUtil.deleteFile(file);
                long startTime = System.currentTimeMillis();
                boolean mergeSuccess = false;
                if (downloadPartFiles.length == 1) {
                    if (FileUtil.renameTo(downloadPartFiles[0], file)) {
                        mergeSuccess = true;
                    }
                } else {
                    if (FileUtil.mergeFiles(downloadPartFiles, file)) {
                        mergeSuccess = true;
                    }
                }
                FileUtil.deleteDir(tempDir);
                if (mergeSuccess) {
                    LogUtil.d("Merge " + downloadInfo.getName() + " spend=" + (System.currentTimeMillis() - startTime) + "; file.length=" + file.length());
                    downloadInfo.setFinished(1);
                    downloadInfo.setCompletedSize(completedSize);
                    downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                } else {
                    LogUtil.e("Merge to " + file.getPath() + " failed.");
                }

            }
        }
        return chain.proceed(downloadRequest);
    }
}
