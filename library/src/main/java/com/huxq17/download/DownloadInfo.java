package com.huxq17.download;


import com.huxq17.download.Utils.Util;

import java.io.File;
import java.util.ArrayList;

public class DownloadInfo {
    private Object tag;
    protected String url;
    protected String filePath;

    protected int progress;
    protected long completedSize;
    protected long contentLength;
    protected int finished = 0;
    protected int status;
    private ArrayList<File> downloadPartFiles = new ArrayList<>();

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

    public int calculateDownloadProgress() {
        if (downloadPartFiles.size() == 0) {
            loadDownloadFiles();
        }
        int completedSize = 0;
        int size = downloadPartFiles.size();
        for (int i = 0; i < size; i++) {
            completedSize += downloadPartFiles.get(i).length();
        }
        int progress = (int) (completedSize * 1f / getContentLength() * 100);
        TransferInfo transferInfo = (TransferInfo) this;
        transferInfo.setProgress(progress);
        transferInfo.setCompletedSize(completedSize);
        return progress;
    }

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }

    private File file;

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        if (file == null) {
            file = new File(filePath);
        }
        return file.getName();
    }

    public long getCompletedSize() {
        return completedSize;
    }

    public long getContentLength() {
        return contentLength;
    }

    public int getProgress() {
        return progress;
    }

    public int getFinished() {
        return finished;
    }

    public boolean isFinished() {
        return finished == 1;
    }

    public Status getStatus() {
        Status status = Status.STOPPED;
        for (Status value : Status.values()) {
            if (value.status == this.status) {
                status = value;
                break;
            }
        }
        return status;
    }

    public enum Status {
        STOPPED(0), RUNNING(1), FINISHED(2), FAILED(3);

        private int status;

        Status(int status) {
            this.status = status;
        }
    }
}
