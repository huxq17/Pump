package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;

public abstract class DownloadObserver {
    private String mObservablePath;

    public DownloadObserver() {
    }

    public DownloadObserver(String observablePath) {
        setObservablePath(observablePath);
    }

    private DownloadInfo downloadInfo;

    public abstract void onProgressUpdate(int progress);

    public abstract void onError(int errorCode);

    public void setObservablePath(String observablePath) {
        this.mObservablePath = observablePath;
    }

    public String getObservablePath() {
        return mObservablePath;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        mObservablePath = downloadInfo.filePath;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
}