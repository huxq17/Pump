package com.huxq17.download.core.task;


import android.text.TextUtils;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadChain;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.interceptor.CheckCacheInterceptor;
import com.huxq17.download.core.interceptor.DownloadFetchInterceptor;
import com.huxq17.download.core.interceptor.MergeFileInterceptor;
import com.huxq17.download.core.interceptor.RetryInterceptor;
import com.huxq17.download.db.DBService;
import com.huxq17.download.message.IMessageCenter;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

public class DownloadTask implements Task {
    private final DownloadDetailsInfo downloadInfo;
    private final Object lock;
    private DBService dbService;
    private IMessageCenter messageCenter;
    private Thread thread;
    private final List<Task> downloadBlockTasks = new ArrayList<>();
    private int lastProgress;
    /**
     * True indicate that not support breakpoint download.
     */
    private DownloadRequest downloadRequest;
    private volatile boolean isDeleted;

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
                    &&downloadRequest.isForceReDownload()) {
                downloadInfo.setCompletedSize(0);
            }
            downloadInfo.setStatus(DownloadInfo.Status.WAIT);
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
    public void run() {
        LogUtil.e("downloadtask run");
        thread = Thread.currentThread();
        if (isRunning()) {
            downloadInfo.setStatus(DownloadInfo.Status.RUNNING);
            notifyProgressChanged(downloadInfo);
            downloadWithDownloadChain();
            notifyProgressChanged(downloadInfo);
        }
        synchronized (downloadBlockTasks) {
            downloadBlockTasks.clear();
        }
    }

    private void downloadWithDownloadChain() {
        List<DownloadInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new RetryInterceptor());
        interceptors.add(new CheckCacheInterceptor());
        interceptors.add(new DownloadFetchInterceptor());
        interceptors.add(new MergeFileInterceptor());
//        interceptors.add(new VerifyResultInterceptor());
        DownloadChain realDownloadChain = new DownloadChain(interceptors, downloadRequest, 0);
        realDownloadChain.proceed(downloadRequest);
        synchronized (lock) {
            if (downloadInfo.getStatus() == DownloadInfo.Status.PAUSING) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
            } else if (isDeleted) {
                FileUtil.deleteDir(downloadInfo.getTempDir());
                FileUtil.deleteFile(downloadInfo.getDownloadFile());
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                DBService.getInstance().deleteInfo(downloadInfo.getId());
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
            if (isRunning()) {
                downloadInfo.setStatus(DownloadInfo.Status.PAUSING);
                notifyProgressChanged(downloadInfo);
                cancel();
            }
        }
    }

    public void stop() {
        synchronized (lock) {
            LogUtil.e("stop downloadInfo.getStatus().shouldStop()=" + downloadInfo.getStatus().shouldStop());
            if (downloadInfo.getStatus().shouldStop()) {
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                cancel();
            }
        }
    }

    public void delete() {
        synchronized (lock) {
            if (isRunning()) {
                isDeleted = true;
                cancel();
            }
        }
    }

    public void cancel() {
        LogUtil.e("cancel downloadBlockTasks.size=" + downloadBlockTasks.size());
        synchronized (downloadBlockTasks) {
            if (thread != null) {
                thread.interrupt();
            }
            for (Task task : downloadBlockTasks) {
                task.cancel();
            }
            downloadBlockTasks.clear();
        }
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void updateInfo() {
        synchronized (lock) {
            if (!isDeleted) {
                dbService.updateInfo(downloadInfo);
            }
        }
    }

    public boolean isRunning() {
        return downloadInfo != null && downloadInfo.isRunning();
    }
}
