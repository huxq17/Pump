package com.huxq17.download;

import com.huxq17.download.Utils.Util;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class DownloadDetailsInfo extends DownloadInfo implements Cloneable {
    private File tempDir;
    private ArrayList<File> downloadPartFiles = new ArrayList<>();
    private File downloadFile;
    private boolean isUsed = false;
    private DownloadTask downloadTask;

    public DownloadDetailsInfo(String url, String filePath) {
        this(url, filePath, null, url);
    }

    public DownloadDetailsInfo(String url, String filePath, String tag, String id) {
        this.url = url;
        this.filePath = filePath;
        this.tag = tag;
        this.id = id;
        if (filePath != null) {
            downloadFile = new File(filePath);
        }
    }

    public void setFilePath(String filePath) {
        if (filePath != null && !filePath.equals(this.filePath)) {
            this.filePath = filePath;
            if (downloadFile != null) {
                Util.deleteFile(downloadFile);
            }
            downloadFile = new File(filePath);
        }
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    @Override
    public DownloadDetailsInfo clone() throws CloneNotSupportedException {
        DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) super.clone();
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
        this.status = Status.RUNNING;
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
        synchronized (this) {
            if (downloadFile == null) {
                return false;
            }
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
    }

    /**
     * load completedSize if not finished.
     */
    private void loadDownloadFiles() {
        if(filePath==null)return;
        File tempDir = Util.getTempDir(filePath);
        File[] listFiles = tempDir.listFiles();
        if (listFiles != null && listFiles.length > 0) {
            downloadPartFiles.addAll(Arrays.asList(listFiles));
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
//            int completedSize = 0;
//            int size = downloadPartFiles.size();
//            for (int i = 0; i < size; i++) {
//                completedSize += downloadPartFiles.get(i).length();
//            }
//            this.completedSize = completedSize;
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
        return downloadFile == null ? "undefined name" : downloadFile.getName();
    }

    @Override
    public String toString() {
        return "DownloadDetailsInfo{" +
                "url='" + url + '\'' +
                ", filePath='" + filePath + '\'' +
                ", id='" + id + '\'' +
                ", completedSize=" + completedSize +
                ", contentLength=" + contentLength +
                ", finished=" + finished +
                ", status=" + status +
                ", errorCode=" + errorCode +
                ", tag='" + tag + '\'' +
                '}';
    }
}
