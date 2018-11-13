package com.huxq17.download.action;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.db.DBService;
import com.huxq17.download.task.DownloadTask;

import java.io.File;

public class CorrectDownloadInfoAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        DownloadTask downloadTask = chain.getDownloadTask();
        TransferInfo downloadInfo = downloadTask.getDownloadInfo();
        DownloadRequest request = downloadTask.getRequest();
        long fileLength = downloadInfo.getContentLength();

        File tempDir = downloadInfo.getTempDir();
        long localLength = DBService.getInstance().queryLocalLength(downloadInfo);
        if (fileLength != localLength) {
            //If file's length have changed,we need to re-download it.
            Util.deleteDir(tempDir);
        }
        downloadInfo.setFinished(0);
        downloadInfo.setCompletedSize(0);
        downloadInfo.setContentLength(fileLength);
        downloadTask.updateInfo(downloadInfo);
//        downloadInfo.threadNum = tempDir.exists() ? downloadInfo.threadNum : oldThreadNum;
        String[] childList = tempDir.list();
        if (childList != null && childList.length != request.getThreadNum()) {
            Util.deleteDir(tempDir);
        }
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }
        return true;
    }
}
