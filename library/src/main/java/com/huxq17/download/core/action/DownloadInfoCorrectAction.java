package com.huxq17.download.core.action;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.FileUtil;

import java.io.File;
import java.io.FilenameFilter;

import static com.huxq17.download.utils.Util.DOWNLOAD_PART;

public class DownloadInfoCorrectAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        DownloadTask downloadTask = chain.getDownloadTask();
        DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
        DownloadRequest request = downloadTask.getRequest();
        long fileLength = downloadInfo.getContentLength();

        File tempDir = downloadInfo.getTempDir();
        long localLength = DBService.getInstance().queryLocalLength(downloadInfo);
        if (fileLength <= 0 || fileLength != localLength) {
            //If file's length have changed,we need to re-download it.
            FileUtil.deleteDir(tempDir);
        }
        downloadInfo.setFinished(0);
        downloadInfo.setCompletedSize(0);
        downloadTask.updateInfo();
//        downloadInfo.threadNum = tempDir.exists() ? downloadInfo.threadNum : oldThreadNum;
        String[] childList = tempDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(DOWNLOAD_PART);
            }
        });
        if (childList != null && childList.length != request.getThreadNum()) {
            FileUtil.deleteDir(tempDir);
        }
        FileUtil.deleteFile(downloadInfo.getDownloadFile());
        return true;
    }
}
