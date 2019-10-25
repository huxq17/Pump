package com.huxq17.download;


import android.text.TextUtils;

import java.lang.ref.WeakReference;

public class DownloadInfo {
    private WeakReference wfExtraData;
    protected String url;
    protected String filePath;
    protected String id;

    protected long completedSize;
    protected long contentLength;
    protected int finished = 0;
    protected Status status;
    protected String speed;
    protected int errorCode;
    protected String tag;
    private long createTime;


    public void setExtraData(Object tag) {
        wfExtraData = new WeakReference<>(tag);
    }

    public Object getExtraData() {
        return wfExtraData == null ? null : wfExtraData.get();
    }

    public String getSpeed() {
        return speed;
    }

    public String getTag() {
        return tag == null ? "" : tag;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
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
        STOPPED, WAIT, PAUSED, PAUSING, RUNNING, FINISHED, FAILED;

        public boolean isRunning() {
            return this == DownloadInfo.Status.WAIT || this == DownloadInfo.Status.RUNNING;
        }
    }
}
