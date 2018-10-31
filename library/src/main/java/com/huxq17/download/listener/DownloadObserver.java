package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;

public abstract class DownloadObserver {
    public DownloadObserver() {
    }

    private DownloadInfo downloadInfo;

    public abstract void onProgress(int progress);

    public abstract void onError(int errorCode);

    public void setDownloadInfo(TransferInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
}