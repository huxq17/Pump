package com.huxq17.download;


import com.huxq17.download.Utils.Util;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.util.ArrayList;

public class TransferInfo extends DownloadInfo implements Cloneable {
    private File tempDir;
    public long createTime;
    private ArrayList<File> downloadPartFiles = new ArrayList<>();
    private File downloadFile;
    private boolean isUsed = false;
    private DownloadTask downloadTask;


    public TransferInfo(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
        downloadFile = new File(filePath);
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    @Override
    public TransferInfo clone() throws CloneNotSupportedException {
        TransferInfo transferInfo = (TransferInfo) super.clone();
        transferInfo.downloadPartFiles = (ArrayList<File>) downloadPartFiles.clone();
        return transferInfo;
    }

    public void setUsed(boolean used) {
        this.isUsed = used;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }

    public void download(int length) {
        completedSize += length;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setErrorCode(int code) {
        this.errorCode = code;
        this.status = Status.FAILED;
    }

    public Status getStatus() {
        return status;
    }

    public File getTempDir() {
        if (tempDir == null) {
            tempDir = Util.getTempDir(filePath);
        }
        return tempDir;
    }

    public boolean isFinished() {
        if (finished == 1) {
            if (downloadFile.exists() && downloadFile.length() == contentLength) {
                return true;
            } else if (downloadFile.exists()) {
                downloadFile.delete();
            }
        }
        finished = 0;
        return false;
    }

    /**
     * load completedSize if not finished.
     */
    private void loadDownloadFiles() {
        File tempDir = Util.getTempDir(filePath);
        File[] listFiles = tempDir.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            for (int i = 0; i < listFiles.length; i++) {
                downloadPartFiles.add(listFiles[i]);
            }
        }
    }

    public void calculateDownloadProgress() {
        if (isFinished()) {
            setCompletedSize(contentLength);
            if (status == null) {
                setStatus(Status.FINISHED);
            }
        } else {
            //Only load once.
            if (downloadPartFiles.size() == 0) {
                loadDownloadFiles();
            }
            int completedSize = 0;
            int size = downloadPartFiles.size();
            for (int i = 0; i < size; i++) {
                completedSize += downloadPartFiles.get(i).length();
            }
            this.completedSize = completedSize;
            if (status == null) {
                setStatus(Status.STOPPED);
            }
        }
//        progress =
    }

    public File getDownloadFile() {
        return downloadFile;
    }

    @Override
    public String getName() {
        return downloadFile.getName();
    }

    @Override
    public String toString() {
        return "TransferInfo{" +
                ", tempDir=" + tempDir +
                ", downloadPartFiles=" + downloadPartFiles +
                ", downloadFile=" + downloadFile +
                ", isUsed=" + isUsed +
                ", url='" + url + '\'' +
                ", filePath='" + filePath + '\'' +
                ", completedSize=" + completedSize +
                ", contentLength=" + contentLength +
                ", finished=" + finished +
                ", status=" + status +
                ", speed='" + speed + '\'' +
                '}';
    }
}
