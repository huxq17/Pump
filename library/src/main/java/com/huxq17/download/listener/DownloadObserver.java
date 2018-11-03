package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;

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

    public final void downloading(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        int progress = downloadInfo.getProgress();
        DownloadInfo.Status status = downloadInfo.getStatus();
        onProgress(progress);
        if (progress == 100) {
            onSuccess();
            ((TransferInfo) downloadInfo).setProgress(0);
        } else if (status == DownloadInfo.Status.FAILED) {
            onFailed();
        }
    }
}