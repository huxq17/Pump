package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;

public abstract class DownloadObserver {
    public DownloadObserver() {
    }

    /**
     * Filter the download information to be received, all received by default.
     *
     * @param downloadInfo The download info.
     * @return Receive if return true, or not receive.
     */
    public boolean filter(DownloadInfo downloadInfo) {
        return true;
    }

    private DownloadInfo downloadInfo;

    public abstract void onProgress(int progress);

    public DownloadInfo.Status getStatus() {
        return DownloadInfo.Status.STOPPED;
    }

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
            ((TransferInfo) downloadInfo).snapshotCompletedSize(0);
        } else if (status == DownloadInfo.Status.FAILED) {
            onFailed();
        }
    }
}