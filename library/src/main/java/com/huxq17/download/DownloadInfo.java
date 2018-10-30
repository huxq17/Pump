package com.huxq17.download;


public class DownloadInfo {
    private Object tag;
    protected String url;
    protected String filePath;

    protected long completedSize;
    protected long contentLength;
    protected int finished = 0;
    protected Status status;
    protected String speed;

    public void setTag(Object tag) {
        this.tag = tag;
    }

    public Object getTag() {
        return tag;
    }

    public String getSpeed() {
        return speed;
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
        if (contentLength == 0) {
            return 0;
        }
        return (int) (completedSize * 1f / contentLength * 100);
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
        WAIT(0), STOPPED(1), RUNNING(2), FINISHED(3), FAILED(4);

        private int status;

        Status(int status) {
            this.status = status;
        }
    }
}
