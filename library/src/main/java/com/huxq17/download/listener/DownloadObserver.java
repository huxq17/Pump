package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;

import java.io.File;
import java.util.ArrayList;

public abstract class DownloadObserver {
    private ArrayList<File> downloadPartFiles = new ArrayList<>();

    public DownloadObserver() {
    }

    private DownloadInfo downloadInfo;

    public abstract void onProgressUpdate(int progress);

    public abstract void onError(int errorCode);

    public void setDownloadInfo(TransferInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
}