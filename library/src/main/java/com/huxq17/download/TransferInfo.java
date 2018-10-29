package com.huxq17.download;


import com.huxq17.download.Utils.Util;

import java.io.File;
import java.util.ArrayList;

public class TransferInfo extends DownloadInfo {
    public int threadNum = 3;
    public boolean forceReDownload = true;
    private File tempDir;
    private ArrayList<File> downloadPartFiles = new ArrayList<>();

    public TransferInfo(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public File getTempDir() {
        if (tempDir == null) {
            tempDir = Util.getTempDir(filePath);
        }
        return tempDir;
    }

    public boolean isFinished() {
        if (finished == 1) {
            File downloadFile = new File(filePath);
            if (downloadFile.exists() && downloadFile.length() == contentLength) {
                return true;
            } else if (downloadFile.exists()) {
                downloadFile.delete();
            }
        }
        return false;
    }

    private void loadDownloadFiles() {
        String filePath = getFilePath();
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

    public int getProgress() {
        calculateDownloadProgress();
        return progress;
    }

    public void calculateDownloadProgress() {
        if (isFinished()) {
            setProgress(100);
            setCompletedSize(contentLength);
            setStatus(Status.FINISHED);
        } else {
            if (progress == 0) {
                if (downloadPartFiles.size() == 0) {
                    loadDownloadFiles();
                }
                int completedSize = 0;
                int size = downloadPartFiles.size();
                for (int i = 0; i < size; i++) {
                    completedSize += downloadPartFiles.get(i).length();
                }
                int progress = (int) (completedSize * 1f / getContentLength() * 100);
                setProgress(progress);
                setCompletedSize(completedSize);
            }
        }
    }
}
