package com.huxq17.download.core.task;


import android.text.TextUtils;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownLoadLifeCycleObserver;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.db.DBService;
import com.huxq17.download.message.IMessageCenter;
import com.huxq17.download.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadTask implements Task {
    private final DownloadDetailsInfo downloadInfo;
    private DBService dbService;
    private AtomicBoolean isRunning;
    private IMessageCenter messageCenter;
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private Thread thread;
    private final List<Task> downloadBlockTasks = new ArrayList<>();
    private int lastProgress;
    /**
     * True indicate that not support breakpoint download.
     */
    private DownloadRequest downloadRequest;
    private final Object lock;
    private volatile boolean isCanceled;
    private volatile boolean isDeleted;
    private boolean supportBreakpoint = true;

    public DownloadTask(DownloadRequest downloadRequest, DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
        if (downloadRequest != null) {
            this.downloadRequest = downloadRequest;
            this.downloadInfo = downloadRequest.getDownloadInfo();
            downloadInfo.setDownloadTask(this);
            isRunning = new AtomicBoolean(true);
            dbService = DBService.getInstance();
            messageCenter = PumpFactory.getService(IMessageCenter.class);
            downloadInfo.setErrorCode(0);
            if (downloadInfo.getCompletedSize() == downloadInfo.getContentLength()) {
                downloadInfo.setCompletedSize(0);
            }
            downloadInfo.setStatus(DownloadInfo.Status.WAIT);
            notifyProgressChanged(downloadInfo);
        } else {
            downloadInfo = null;
        }
        lock = downloadInfo;
    }

    public boolean isSupportBreakpoint() {
        return supportBreakpoint;
    }

    public void setSupportBreakpoint(boolean supportBreakpoint) {
        this.supportBreakpoint = supportBreakpoint;
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
    public void run() {
        thread = Thread.currentThread();
        if (isRunning.get()) {
            downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
            notifyProgressChanged(downloadInfo);
            downLoadLifeCycleObserver.onDownloadStart(this);
            long startTime = System.currentTimeMillis();
            downloadWithDownloadChain();
            LogUtil.d("download " + downloadInfo.getName() + " spend=" + (System.currentTimeMillis() - startTime));
        }
        isRunning.set(false);
        thread = null;
        downLoadLifeCycleObserver.onDownloadEnd(this);
        synchronized (downloadBlockTasks) {
            downloadBlockTasks.clear();
        }
    }

    private void downloadWithDownloadChain() {
        DownloadChain chain = new DownloadChain(this);
        chain.proceed();
    }

    boolean onDownload(int length) {
        synchronized (lock) {
            if (!isRunning.get()) {
                return false;
            }
            downloadInfo.download(length);
            int progress = (int) (downloadInfo.getCompletedSize() * 1f / downloadInfo.getContentLength() * 100);
            if (progress != lastProgress) {
                if (progress != 100) {
                    lastProgress = progress;
                    downloadInfo.computeSpeed();
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

    public void addBlockTask(Task task) {
        synchronized (downloadBlockTasks) {
            downloadBlockTasks.add(task);
        }
    }

    public void pause() {
        synchronized (lock) {
            if (isRunning.get()) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSING);
                notifyProgressChanged(downloadInfo);
                cancel();
            }
        }
    }

    public void stop() {
        synchronized (lock) {
            if (isRunning.get()) {
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                downloadInfo.setDownloadTask(null);
                cancel();
            }
        }
    }

    void cancelBlockTasks() {
        if (thread != null) {
            thread.interrupt();
        } else {
            downLoadLifeCycleObserver.onDownloadEnd(this);
        }
        synchronized (downloadBlockTasks) {
            for (Task task : downloadBlockTasks) {
                task.cancel();
            }
            downloadBlockTasks.clear();
        }
    }

    public void cancel() {
        if (isCanceled) return;
        destroy();
        isCanceled = true;
        cancelBlockTasks();
    }

    public void delete() {
        synchronized (lock) {
            if (isRunning.get()) {
                isDeleted = true;
                cancel();
            }
        }
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public void setErrorCode(int errorCode) {
        if (downloadInfo.getStatus() != DownloadInfo.Status.PAUSING) {
            downloadInfo.setErrorCode(errorCode);
        }
    }

    public void updateInfo() {
        synchronized (lock) {
            if (!isDeleted) {
                dbService.updateInfo(downloadInfo);
            }
        }
    }

    public void destroy() {
        isRunning.set(false);
    }

    public boolean isRunning() {
        return isRunning.get();
    }
}
