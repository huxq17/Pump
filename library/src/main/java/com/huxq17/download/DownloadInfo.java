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
    public boolean forceRestart = true;
    private File tempDir;

    public File getTempDir() {
        if (tempDir == null) {
            tempDir = Util.getTempDir(filePath);
        }
        return tempDir;
    }

    public boolean isFinished() {
        return finished == 1;
    }
}
