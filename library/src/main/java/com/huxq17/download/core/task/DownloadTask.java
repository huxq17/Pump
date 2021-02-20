package com.huxq17.download.core.task;


import android.text.TextUtils;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.RealDownloadChain;
import com.huxq17.download.core.interceptor.ConnectInterceptor;
import com.huxq17.download.core.interceptor.MergeFileInterceptor;
import com.huxq17.download.core.interceptor.RetryInterceptor;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.service.IMessageCenter;
import com.huxq17.download.db.DBService;

import java.util.ArrayList;
import java.util.List;

public class DownloadTask extends Task {
    private final DownloadDetailsInfo downloadInfo;
    private final Object lock;
    private DBService dbService;
    private IMessageCenter messageCenter;
    private int lastProgress;
    private DownloadRequest downloadRequest;
    private ConnectInterceptor connectInterceptor;

    public DownloadTask(DownloadRequest downloadRequest) {
        if (downloadRequest != null) {
            this.downloadRequest = downloadRequest;
            this.downloadInfo = downloadRequest.getDownloadInfo();
            lock = downloadInfo;
            downloadInfo.setDownloadTask(this);
            dbService = DBService.getInstance();
            messageCenter = PumpFactory.getService(IMessageCenter.class);
            downloadInfo.clearErrorCode();
            downloadInfo.setStatus(DownloadInfo.Status.WAIT);
            downloadInfo.setCompletedSize(0);
            downloadInfo.setProgress(0);
            if (downloadInfo.getCompletedSize() > 0 &&
                    downloadInfo.getCompletedSize() == downloadInfo.getContentLength()
                    && downloadRequest.isForceReDownload()) {
                updateInfo();
            }
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

    public String getDownloadId() {
        return downloadRequest.getId();
    }

    public String getDownloadName() {
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
        downloadInfo.setDownloadTask(null);
    }

    private void downloadWithDownloadChain() {
        List<DownloadInterceptor> interceptors = new ArrayList<>(PumpFactory.getService(IDownloadConfigService.class)
                .getDownloadInterceptors());
        connectInterceptor = new ConnectInterceptor();
        interceptors.add(new RetryInterceptor());
        interceptors.add(connectInterceptor);
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
            if (progress < 0) {
                progress = 0;
            }
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

    public void pauseDownload() {
        synchronized (lock) {
            if (isRunning()) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSING);
                notifyProgressChanged(downloadInfo);
                cancel();
            }
        }
    }

    public void stopDownload() {
        synchronized (lock) {
            if (downloadInfo.getStatus().shouldStop()) {
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                cancel();
            }
        }
    }

    public void cancel() {
        if (connectInterceptor != null) {
            connectInterceptor.cancel();
        }
        if(currentThread!=null){
            currentThread.interrupt();
        }
    }

    public void updateInfo() {
        synchronized (lock) {
            dbService.updateInfo(downloadInfo);
        }
    }

    public boolean isRunning() {
        return downloadInfo != null && downloadInfo.isRunning();
    }
}
