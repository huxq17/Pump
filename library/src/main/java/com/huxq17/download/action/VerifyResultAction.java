package com.huxq17.download.action;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.db.DBService;
import com.huxq17.download.task.DownloadTask;

public class VerifyResultAction implements Action {
    @Override
    public boolean proceed(DownloadTask t) {
        TransferInfo downloadInfo = t.getDownloadInfo();
        synchronized (downloadInfo) {
            DownloadInfo.Status status = downloadInfo.getStatus();
            t.destroy();
            if (t.isNeedDelete()) {
                Util.deleteDir(downloadInfo.getTempDir());
                downloadInfo.getDownloadFile().delete();
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                DBService.getInstance().deleteInfo(downloadInfo.getUrl(), downloadInfo.getFilePath());
            } else {
                if (status == DownloadInfo.Status.PAUSING) {
                    downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
                }
                //
                if (downloadInfo.getCompletedSize() == downloadInfo.getContentLength()) {
                    downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                }
                t.notifyProgressChanged(downloadInfo);
            }
        }
        return true;
    }
}
