package com.huxq17.download.listener;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Utils.Util;

import java.io.File;
import java.util.ArrayList;

public abstract class DownloadObserver {
    private String mObservablePath;
    private ArrayList<File> downloadPartFiles = new ArrayList<>();

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

    private void loadDownloadFiles() {
        File file = new File(mObservablePath);
        if (file.isFile()) {
            File tempDir = Util.getTempDir(mObservablePath);
            File[] listFiles = tempDir.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (int i = 0; i < listFiles.length; i++) {
                    downloadPartFiles.add(listFiles[i]);
                }
            } else if (file.isDirectory()) {

            }
        }
    }

    public String getObservablePath() {
        return mObservablePath;
    }

    public void setDownloadInfo(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        setObservablePath(downloadInfo.filePath);
    }

    public int calculateDownloadProgress() {
        if (downloadPartFiles.size() == 0) {
            loadDownloadFiles();
        }
        int completedSize = 0;
        int size = downloadPartFiles.size();
        for (int i = 0; i < size; i++) {
            completedSize += downloadPartFiles.get(i).length();
        }
        int progress = (int) (completedSize * 1f / downloadInfo.contentLength * 100);
        return progress;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
}