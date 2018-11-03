package com.huxq17.download.action;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.task.DownloadTask;

import java.io.File;

public class MergeFileAction implements Action {
    @Override
    public boolean proceed(DownloadTask t) {
        TransferInfo downloadInfo = t.getDownloadInfo();
        long fileLength = downloadInfo.getContentLength();
        File tempDir = downloadInfo.getTempDir();
        File file = downloadInfo.getDownloadFile();
        if (file.exists()) {
            file.delete();
        }
        long completedSize = downloadInfo.getCompletedSize();
        long startTime = System.currentTimeMillis();
        if (completedSize == fileLength) {
            File[] downloadPartFiles = tempDir.listFiles();
            if (downloadPartFiles != null && downloadPartFiles.length > 0) {
                Util.mergeFiles(downloadPartFiles, file);
                Util.deleteDir(tempDir);
            }
            LogUtil.e("merge" + downloadInfo.getName() + " spend=" + (System.currentTimeMillis() - startTime));
            downloadInfo.setFinished(1);
            downloadInfo.setCompletedSize(completedSize);
            t.updateInfo(downloadInfo);
            downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
        } else {
            downloadInfo.setStatus(DownloadInfo.Status.FAILED);
        }
        return true;
    }
}
