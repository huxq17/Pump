package com.huxq17.download.core;

import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.huxq17.download.Pump;
import com.huxq17.download.android.ViewLifecycleHandler;

public class DownloadListener {
    private String id;
    private DownloadInfo.Status status;
    private boolean enable;

    public DownloadListener() {
    }

    public DownloadListener(FragmentActivity activity) {
        ViewLifecycleHandler.handleLifecycle(activity.getLifecycle(), this);
    }

    public DownloadListener(Fragment fragment) {
        ViewLifecycleHandler.handleLifecycleForFragment(fragment, this);
    }

    /**
     * Disable this observer and Pump will remove this observer later.
     */
    public final void disable() {
        Pump.unSubscribe(this);
    }

    void setEnable(boolean enable) {
        this.enable = enable;
    }

    void setId(String id) {
        this.id = id;
    }

    /**
     * Enable this observer.
     */
    public final void enable() {
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
        DownloadInfo.Status status = downloadInfo.getStatus();
        this.downloadInfo = downloadInfo;
        this.status = status;
        int progress = downloadInfo.getProgress();
        onProgress(progress);
        if (status == DownloadInfo.Status.FAILED) {
            onFailed();
            unSubscribe();
        } else if (downloadInfo.getStatus() == DownloadInfo.Status.FINISHED) {
            onSuccess();
            unSubscribe();
        }
    }

    private void unSubscribe() {
        if (id != null) {
            Pump.unSubscribe(id);
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
        return id == null || id.equals(downloadInfo.getId());
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

    @Override
    public String toString() {
        return "DownloadListener{" +
                "id='" + id + '\'' +
                '}';
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