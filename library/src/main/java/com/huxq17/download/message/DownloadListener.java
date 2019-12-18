package com.huxq17.download.message;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.huxq17.download.Pump;
import com.huxq17.download.core.DownloadInfo;

import java.lang.ref.WeakReference;

public class DownloadListener {
    private String id;
    private DownloadInfo.Status status;
    private boolean enable;
    private final WeakReference<Object> reference;

    public DownloadListener() {
        this(null);
    }

    public DownloadListener(Activity reference) {
        this.reference = new WeakReference<Object>(reference);
    }

    /**
     * Disable this observer and Pump will remove this observer later.
     */
    public final void disable() {
        Pump.unSubscribe(this);
    }

    public WeakReference getReference() {
        return reference;
    }

    void setEnable(boolean enable) {
        this.enable = enable;
    }

    void setId(String id) {
        this.id = id;
    }

    /**
     * Enable this Observer.
     */
    public final void enable() {
        enable(null);
    }

    public final void enable(String id) {
        this.id = id;
        if (!enable) {
            Pump.subscribe(this);
        }
    }

    public final boolean isEnable() {
        return enable;
    }

    private DownloadInfo downloadInfo;


    public final DownloadInfo.Status getStatus() {
        return status;
    }

    public final DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }

    final void downloading(DownloadInfo downloadInfo) {
        long completedSize = downloadInfo.getCompletedSize();
        DownloadInfo.Status status = downloadInfo.getStatus();
        this.downloadInfo = downloadInfo;
        this.status = status;
        long contentLength = downloadInfo.getContentLength();
        int progress = contentLength == 0 ? 0 : (int) (completedSize * 1f / contentLength * 100);
        onProgress(progress);
        if (progress == 100) {
            onSuccess();
        } else if (status == DownloadInfo.Status.FAILED) {
            onFailed();
        }
    }

    public String getId() {
        return id;
    }

    /**
     * Filter the download information to be received, all received by default.
     *
     * @param downloadInfo The download info.
     * @return Receive if return true, or not receive.
     */
    public boolean filter(DownloadInfo downloadInfo) {
        if (!TextUtils.isEmpty(id)) {
            return id.equals(downloadInfo.getId());
        }
        return true;
    }

    public void onProgress(int progress) {
    }


    public void onSuccess() {
    }

    public void onFailed() {
    }

    @Override
    public int hashCode() {
        if (!TextUtils.isEmpty(id)) {
            return id.hashCode();
        }
        return super.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "Listener for " + id + " download task.";
    }

    @Override
    public boolean equals(Object obj) {
        if (DownloadListener.class.isAssignableFrom(obj.getClass())) {
            DownloadListener that = (DownloadListener) obj;
            String thatId = that.getId();
            if (!TextUtils.isEmpty(thatId) && thatId.equals(getId())) {
                return true;
            }
        }
        return super.equals(obj);
    }
}