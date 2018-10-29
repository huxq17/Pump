package com.huxq17.download;


import java.io.File;

public class DownloadInfo {
    private Object tag;
    protected String url;
    protected String filePath;

    protected int progress;
    protected long completedSize;
    protected long contentLength;
    protected int finished = 0;
    protected Status status;

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
        return status;
    }

    public enum Status {
        WAIT(0),STOPPED(1), RUNNING(2), FINISHED(3), FAILED(4);

        private int status;

        Status(int status) {
            this.status = status;
        }
    }
}
