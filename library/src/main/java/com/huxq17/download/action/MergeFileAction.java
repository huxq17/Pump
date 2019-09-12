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
        synchronized (downloadTask.getLock()) {
            DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
            long fileLength = downloadInfo.getContentLength();
            File tempDir = downloadInfo.getTempDir();
            File file = downloadInfo.getDownloadFile();
            if (file.exists()) {
                file.delete();
            }
            long completedSize = downloadInfo.getCompletedSize();
            File[] downloadPartFiles = tempDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith(DOWNLOAD_PART);
                }
            });
            if (fileLength > 0 && completedSize == fileLength && downloadPartFiles != null && downloadPartFiles.length == downloadTask.getRequest().getThreadNum()) {
                long startTime = System.currentTimeMillis();
                if (downloadPartFiles.length == 1) {
                    if (!downloadPartFiles[0].renameTo(file)) {
                        downloadInfo.setStatus(DownloadInfo.Status.FAILED);
                        Util.deleteDir(tempDir);
                        LogUtil.e("rename "+downloadPartFiles[0].getPath()+" to "+file.getPath()+" failed.");
                        return false;
                    } else {
                        Util.deleteDir(tempDir);
                    }
                } else {
                    Util.mergeFiles(downloadPartFiles, file);
                    Util.deleteDir(tempDir);
                }
                LogUtil.i("merge " + downloadInfo.getName() + " spend=" + (System.currentTimeMillis() - startTime)+"; file.length="+ file.length());
                downloadInfo.setFinished(1);
                downloadInfo.setCompletedSize(completedSize);
                downloadTask.updateInfo();
                downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
            } else {
                downloadInfo.setStatus(DownloadInfo.Status.FAILED);
            }
        }
        return true;
    }
}
