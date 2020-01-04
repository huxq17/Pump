package com.huxq17.download.core.task;


import android.text.TextUtils;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.RealDownloadChain;
import com.huxq17.download.core.interceptor.CheckCacheInterceptor;
import com.huxq17.download.core.interceptor.DownloadFetchInterceptor;
import com.huxq17.download.core.interceptor.MergeFileInterceptor;
import com.huxq17.download.core.interceptor.RetryInterceptor;
import com.huxq17.download.db.DBService;
import com.huxq17.download.message.IMessageCenter;

import java.util.ArrayList;
import java.util.List;

public class DownloadTask extends Task {
    private final DownloadDetailsInfo downloadInfo;
    private final Object lock;
    private DBService dbService;
    private IMessageCenter messageCenter;
    private int lastProgress;
    private DownloadRequest downloadRequest;
    private DownloadFetchInterceptor fetchInterceptor;

    public DownloadTask(DownloadRequest downloadRequest) {
        if (downloadRequest != null) {
            this.downloadRequest = downloadRequest;
            this.downloadInfo = downloadRequest.getDownloadInfo();
            lock = downloadInfo;
            downloadInfo.setDownloadTask(this);
            dbService = DBService.getInstance();
            messageCenter = PumpFactory.getService(IMessageCenter.class);
            downloadInfo.setErrorCode(0);
            if (downloadInfo.getCompletedSize() == downloadInfo.getContentLength()
                    && downloadRequest.isForceReDownload()) {
                downloadInfo.setCompletedSize(0);
                downloadInfo.deleteDownloadFile();
            }
            downloadInfo.setStatus(DownloadInfo.Status.WAIT);
            downloadInfo.setProgress(0);
            updateInfo();
            notifyProgressChanged(downloadInfo);
        } else {
            downloadInfo = null;
            lock = null;
        }
    }

    public Object getLock() {
        return lock;
    }

    public DownloadRequest getRequest() {
        return downloadRequest;
    }

    public String getUrl() {
        return downloadRequest.getUrl();
    }

    public String getId() {
        return downloadRequest.getId();
    }

    public String getName() {
        String name = downloadRequest.getDownloadInfo().getName();
        if (TextUtils.isEmpty(name)) {
            name = downloadRequest.getName();
        }
        return name;
    }

    @Override
    public void execute() {
        if (isRunning()) {
            downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
            notifyProgressChanged(downloadInfo);
            downloadWithDownloadChain();
            notifyProgressChanged(downloadInfo);
        }
    }

    private void downloadWithDownloadChain() {
        List<DownloadInterceptor> interceptors = new ArrayList<>(PumpFactory.getService(IDownloadConfigService.class)
                .getDownloadInterceptors());
        fetchInterceptor = new DownloadFetchInterceptor();
        interceptors.add(new RetryInterceptor());
        interceptors.add(new CheckCacheInterceptor());
        interceptors.add(fetchInterceptor);
        interceptors.add(new MergeFileInterceptor());
        RealDownloadChain realDownloadChain = new RealDownloadChain(interceptors, downloadRequest, 0);
        realDownloadChain.proceed(downloadRequest);
        synchronized (lock) {
            if (downloadInfo.getStatus() == DownloadInfo.Status.PAUSING) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
            }
        }
        updateInfo();
    }

    boolean onDownload(int length) {
        synchronized (lock) {
            if (!isRunning()) {
                return false;
            }
            downloadInfo.download(length);
            int progress = (int) (downloadInfo.getCompletedSize() * 1f / downloadInfo.getContentLength() * 100);
            downloadInfo.setProgress(progress);
            if (progress != lastProgress) {
                if (progress != 100) {
                    lastProgress = progress;
                    notifyProgressChanged(downloadInfo);
                }
            }
        }
        return true;
    }

    public void notifyProgressChanged(DownloadDetailsInfo downloadInfo) {
        if (messageCenter != null)
            messageCenter.notifyProgressChanged(downloadInfo);
    }

    public DownloadDetailsInfo getDownloadInfo() {
        return downloadInfo;
    }

    public void pause() {
        synchronized (lock) {
            if (isRunning()) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSING);
                notifyProgressChanged(downloadInfo);
                cancel();
            }
        }
    }

    public void stop() {
        synchronized (lock) {
            if (downloadInfo.getStatus().shouldStop()) {
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                cancel();
            }
        }
    }

    public void cancel() {
        if (fetchInterceptor != null) {
            fetchInterceptor.cancel();
        }
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

    public void updateInfo() {
        synchronized (lock) {
            dbService.updateInfo(downloadInfo);
        }
    }

    public boolean isRunning() {
        return currentThread != null && downloadInfo != null && downloadInfo.isRunning();
    }
}
