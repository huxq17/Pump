package com.huxq17.download.listener;

public interface DownloadStatus {
    void onDownload(int threadId, int length, long downloadedSize);
    boolean isStopped();
}
