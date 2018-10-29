package com.huxq17.download.listener;

import android.util.Log;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.Utils.Util;

import java.io.File;
import java.util.ArrayList;

public abstract class DownloadObserver {
    private ArrayList<File> downloadPartFiles = new ArrayList<>();

    public DownloadObserver() {
    }

    private DownloadInfo downloadInfo;

    public abstract void onProgressUpdate(int progress);

    public abstract void onError(int errorCode);


    private void loadDownloadFiles() {
        String filePath = downloadInfo.getFilePath();
        File file = new File(filePath);
        if (file.exists()) {
            File tempDir = Util.getTempDir(filePath);
            File[] listFiles = tempDir.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (int i = 0; i < listFiles.length; i++) {
                    downloadPartFiles.add(listFiles[i]);
                }
            }
        }
    }


    public void setDownloadInfo(TransferInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
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
        int progress = (int) (completedSize * 1f / downloadInfo.getContentLength() * 100);
        TransferInfo transferInfo = (TransferInfo) downloadInfo;
        transferInfo.setProgress(progress);
        transferInfo.setCompletedSize(completedSize);
        Log.e("tag", "calculateDownloadProgress progress=" + progress + ";completedSize=");
        return progress;
    }

    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
}