package com.huxq17.download.core.action;


import com.huxq17.download.core.DownloadChain;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;

import java.io.File;
import java.io.FilenameFilter;

import static com.huxq17.download.utils.Util.DOWNLOAD_PART;

public class FileMergeAction implements Action {
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
