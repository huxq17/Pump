package com.huxq17.download.core;

import android.content.Context;
import android.os.Environment;
import android.text.format.Formatter;

import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.TaskManager;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.core.task.Task;
import com.huxq17.download.db.DBService;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.IMessageCenter;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class DownloadDispatcher extends Task {
    private DownloadManager downloadManager;
    private AtomicBoolean isRunning = new AtomicBoolean();
    private AtomicBoolean isCanceled = new AtomicBoolean();
    private ConcurrentLinkedQueue<DownloadRequest> requestQueue;

    private Lock lock = new ReentrantLock();
    private Condition consumer = lock.newCondition();
    private HashSet<DownloadTaskExecutor> downloadTaskExecutors = new HashSet<>(1);
    private DownloadTaskExecutor defaultTaskExecutor;
    private DownloadInfoManager downloadInfoManager;

    DownloadDispatcher(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void start() {
        isRunning.set(true);
        isCanceled.set(false);
        requestQueue = new ConcurrentLinkedQueue<>();
        TaskManager.execute(this);
        downloadInfoManager = DownloadInfoManager.getInstance();
        defaultTaskExecutor = new SimpleDownloadTaskExecutor();
    }

    void enqueueRequest(final DownloadRequest request) {
        if (!isRunning()) {
            start();
        }
        if (isRunning.get()) {
            if (!requestQueue.contains(request)) {
                requestQueue.add(request);
                signalConsumer();
            } else {
                printExistRequestWarning(request);
            }
        }
    }

    void consumeRequest() {
        waitForConsumer();
        DownloadRequest downloadRequest = requestQueue.poll();
        if (downloadRequest != null && !downloadManager.isTaskRunning(downloadRequest.getId())) {
            DownloadTaskExecutor downloadTaskExecutor = downloadRequest.getDownloadExecutor();
            if (downloadTaskExecutor == null) {
                downloadTaskExecutor = this.defaultTaskExecutor;
            }
            if (!downloadTaskExecutors.contains(downloadTaskExecutor)) {
                downloadTaskExecutor.init();
                downloadTaskExecutors.add(downloadTaskExecutor);
            }

            DownloadTask downloadTask = getTaskFromRequest(downloadRequest);
            if (downloadTask != null) {
                downloadTaskExecutor.execute(downloadTask);
            }
        }
    }

    @Override
    public void execute() {
        while (isRunnable()) {
            consumeRequest();
        }
        isRunning.set(false);
    }


    public boolean isRunning() {
        return isRunning.get();
    }

    void setIsRunning(boolean isRunning) {
        this.isRunning.set(isRunning);
    }

    @Override
    public void cancel() {
        isCanceled.set(true);
        signalConsumer();
        downloadTaskExecutors.clear();
        if (defaultTaskExecutor != null) {
            defaultTaskExecutor.shutdown();
        }
    }

    boolean isBlockForConsumeRequest() {
        return requestQueue.isEmpty();
    }

    void waitForConsumer() {
        lock.lock();
        try {
            if (isBlockForConsumeRequest()) {
                consumer.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    boolean isRunnable() {
        return isRunning() && !isCanceled.get();
    }

    void signalConsumer() {
        lock.lock();
        try {
            consumer.signal();
        } finally {
            lock.unlock();
        }
    }

    void printExistRequestWarning(DownloadRequest request) {
        LogUtil.w("task " + request.getName() + " already enqueue,we need do nothing.");
    }

    DownloadTask getTaskFromRequest(DownloadRequest downloadRequest) {
        String url = downloadRequest.getUrl();
        String id = downloadRequest.getId();
        String tag = downloadRequest.getTag();
        String filePath = downloadRequest.getFilePath();
        if (!isUsableSpaceEnough(downloadRequest)) {
            return null;
        }
        DownloadDetailsInfo downloadInfo = downloadRequest.getDownloadInfo();
        if (downloadInfo == null) {
            downloadInfo = createDownloadInfo(id, url, filePath, tag);
            downloadRequest.setDownloadInfo(downloadInfo);
        }
        downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
        return new DownloadTask(downloadRequest);

    }

    boolean isUsableSpaceEnough(DownloadRequest downloadRequest) {
        long downloadDirUsableSpace;
        String filePath = downloadRequest.getFilePath();
        if (filePath == null) {
            downloadDirUsableSpace = Util.getUsableSpace(new File(Util.getCachePath(PumpFactory.getService(IDownloadManager.class).getContext())));
        } else {
            downloadDirUsableSpace = Util.getUsableSpace(new File(filePath));
        }
        long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
        long minUsableStorageSpace = getMinUsableStorageSpace();
        if (downloadDirUsableSpace <= minUsableStorageSpace || dataFileUsableSpace <= minUsableStorageSpace) {
            Context context = PumpFactory.getService(IDownloadManager.class).getContext();
            String dataFileAvailableSize = Formatter.formatFileSize(context, dataFileUsableSpace);
            String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
            LogUtil.e("Data directory usable space is " + dataFileAvailableSize + " and download directory usable space is " + downloadFileAvailableSize);
            DownloadDetailsInfo downloadInfo = downloadInfoManager.createDownloadInfo(downloadRequest.getUrl(),
                    filePath, downloadRequest.getTag(), downloadRequest.getId(), System.currentTimeMillis(), false);
            downloadInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
            PumpFactory.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
            return false;
        }
        return true;
    }

    long getMinUsableStorageSpace() {
        return PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
    }

    DownloadDetailsInfo createDownloadInfo(String id, String url, String filePath, String tag) {
        DownloadDetailsInfo downloadInfo = DBService.getInstance().getDownloadInfo(id);
        if (downloadInfo != null) {
            return downloadInfo;
        }
        //create a new instance if not found.
        downloadInfo = downloadInfoManager.createDownloadInfo(url, filePath, tag, id, System.currentTimeMillis());
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }
}