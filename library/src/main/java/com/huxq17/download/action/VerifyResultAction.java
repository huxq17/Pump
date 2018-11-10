package com.huxq17.download.action;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.db.DBService;
import com.huxq17.download.task.DownloadTask;

public class VerifyResultAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        DownloadTask downloadTask = chain.getDownloadTask();
        TransferInfo downloadInfo = downloadTask.getDownloadInfo();
        synchronized (downloadInfo) {
            DownloadInfo.Status status = downloadInfo.getStatus();
            downloadTask.destroy();
            if (downloadTask.isNeedDelete()) {
                Util.deleteDir(downloadInfo.getTempDir());
                downloadInfo.getDownloadFile().delete();
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                DBService.getInstance().deleteInfo(downloadInfo.getUrl(), downloadInfo.getFilePath());
            } else {
                if (status == DownloadInfo.Status.PAUSING) {
                    downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
                }
                long completedSize = downloadInfo.getCompletedSize();
                long contentLength = downloadInfo.getContentLength();
                if (completedSize != 0 && completedSize == contentLength) {
                    downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                }
                downloadTask.notifyProgressChanged(downloadInfo);
            }
        }
        return true;
    }
}
