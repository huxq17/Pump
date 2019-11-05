package com.huxq17.download.action;


import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.io.FilenameFilter;

import static com.huxq17.download.DownloadBatch.DOWNLOAD_PART;


public class MergeFileAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        DownloadTask downloadTask = chain.getDownloadTask();
        if (!downloadTask.isSupportBreakpoint()) {
            return true;
        }
        synchronized (downloadTask.getLock()) {
            DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
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
                Util.deleteFile(file);
                long startTime = System.currentTimeMillis();
                boolean mergeSuccess = false;
                if (downloadPartFiles.length == 1) {
                    if (Util.renameTo(downloadPartFiles[0], file)) {
                        mergeSuccess = true;
                    }
                } else {
                    if (Util.mergeFiles(downloadPartFiles, file)) {
                        mergeSuccess = true;
                    }
                }
                Util.deleteDir(tempDir);
                if (mergeSuccess) {
                    LogUtil.d("Merge " + downloadInfo.getName() + " spend=" + (System.currentTimeMillis() - startTime) + "; file.length=" + file.length());
                    downloadInfo.setFinished(1);
                    downloadInfo.setCompletedSize(completedSize);
                    downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                } else {
                    downloadInfo.setStatus(DownloadInfo.Status.FAILED);
                    LogUtil.e("Merge to " + file.getPath() + " failed.");
                }

            } else {
                downloadInfo.setStatus(DownloadInfo.Status.FAILED);
            }
        }
        return true;
    }
}
