package com.huxq17.download;


import com.huxq17.download.Utils.Util;

import java.io.File;

public class DownloadInfo {
    public String url;
    public String filePath;

    public int progress;
    public long completedSize;
    public long contentLength;
    public int threadNum = 3;
    public int finished = 0;
    public boolean forceReDownload = true;
    private File tempDir;

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
