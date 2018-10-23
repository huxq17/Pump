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

    public File getTempFile() {
        return new File(filePath.concat(".temp"));
    }

    public boolean isFinished() {
        return finished == 1;
    }
}
