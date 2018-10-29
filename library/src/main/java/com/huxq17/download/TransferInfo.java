package com.huxq17.download;


import com.huxq17.download.Utils.Util;

import java.io.File;

public class TransferInfo extends DownloadInfo {
    public int threadNum = 3;
    public boolean forceReDownload = true;
    private File tempDir;

    public TransferInfo(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public File getTempDir() {
        if (tempDir == null) {
            tempDir = Util.getTempDir(filePath);
        }
        return tempDir;
    }

    public boolean isFinished() {
        if (finished == 1) {
            File downloadFile = new File(filePath);
            if (downloadFile.exists() && downloadFile.length() == contentLength) {
                return true;
            } else if (downloadFile.exists()) {
                downloadFile.delete();
            }
        }
        return false;
    }
}
