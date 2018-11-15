package com.huxq17.download;


import java.lang.ref.WeakReference;

public class DownloadInfo {
    private WeakReference wfTag;
    protected String url;
    protected String filePath;

    protected long completedSize;
    protected long contentLength;
    protected int finished = 0;
    protected Status status;
    protected String speed;
    protected int errorCode;


    public void setTag(Object tag) {
        wfTag = new WeakReference<>(tag);
    }

    public Object getTag() {
        return wfTag == null ? null : wfTag.get();
    }

    public String getSpeed() {
        return speed;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return null;
    }

    public long getCompletedSize() {
        return completedSize;
    }

    public long getContentLength() {
        return contentLength;
    }

    public int getProgress() {
        return contentLength == 0 ? 0 : (int) (completedSize * 1f / contentLength * 100);
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
        STOPPED, WAIT, PAUSED, PAUSING, RUNNING, FINISHED, FAILED
    }
}
