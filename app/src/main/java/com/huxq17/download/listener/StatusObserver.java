package com.huxq17.download.listener;


import com.huxq17.download.DownloadInfo;

public abstract class StatusObserver {
    private DownloadInfo downloadInfo;

    public abstract void onProgressUpdate(int progress);

    public abstract void onError(int errorCode);

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
}
