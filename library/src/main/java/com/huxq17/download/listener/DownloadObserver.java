package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;

public abstract class DownloadObserver {
    private int lastProgress = -1;

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
        DownloadInfo.Status status = downloadInfo.getStatus();
        int progress = downloadInfo.getProgress();
        if (progress == 100 && DownloadInfo.Status.FINISHED != downloadInfo.getLastStatus() || progress != 100) {
            downloadInfo.setLastStatus(status);
            onProgress(progress);
            if (status == DownloadInfo.Status.FINISHED) {
                onSuccess();
            } else if (status == DownloadInfo.Status.PAUSING) {
                onFailed();
            }
        }
    }
}