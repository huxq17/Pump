package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;

public abstract class DownloadObserver {
    public DownloadObserver() {
    }

    private DownloadInfo downloadInfo;

    public abstract void onProgress(int progress);

    public void onSuccess() {
    }

    public void onFailed() {
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    public final void downloading(int progress, DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        DownloadInfo.Status status = downloadInfo.getStatus();
        onProgress(progress);
        if (progress == 100) {
            onSuccess();
        } else if (status == DownloadInfo.Status.FAILED) {
            onFailed();
        }
    }
}