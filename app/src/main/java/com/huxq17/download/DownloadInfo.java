package com.huxq17.download;


import com.huxq17.download.listener.StatusObserver;

import java.io.File;

public class DownloadInfo {
    public String url;
    public String filePath;
    public StatusObserver statusObserver;

    public int progress;
    public long downloadLength;
    public long contentLength;
    public int threadNum = 3;
    public int finished = 0;
    public boolean forceRestart = true;
    private File tempDir;

    public File getTempDir() {
        if (tempDir == null) {
            File file = new File(filePath);
            File parentFile = file.getParentFile();
            tempDir = new File(parentFile, "." + file.getName() + ".temp" + File.separatorChar);
        }
        return tempDir;
    }

    public boolean isFinished() {
        return finished == 1;
    }
}
