package com.huxq17.download.core;

import android.content.Context;
import android.net.Uri;
import android.text.format.Formatter;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.service.IMessageCenter;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.Util;

import java.util.HashSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class DownloadDispatcher extends Thread {
    private DownloadManager downloadManager;
    private AtomicBoolean isCanceled = new AtomicBoolean();
    private final ConcurrentLinkedQueue<DownloadRequest> requestQueue = new ConcurrentLinkedQueue<>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition consumer = lock.newCondition();
    private HashSet<DownloadTaskExecutor> downloadTaskExecutors = new HashSet<>(1);
    private DownloadTaskExecutor defaultTaskExecutor;
    private DownloadInfoManager downloadInfoManager;

    DownloadDispatcher(DownloadManager downloadManager) {
        this.downloadManager = downloadManager;
    }

    public void start() {
        if (isAlive()) {
            return;
        }
        isCanceled.getAndSet(false);
        super.start();
        downloadInfoManager = DownloadInfoManager.getInstance();
        defaultTaskExecutor = new SimpleDownloadTaskExecutor();
    }

    void enqueueRequest(final DownloadRequest downloadRequest) {
        start();
        if (!requestQueue.contains(downloadRequest)) {
            requestQueue.add(downloadRequest);
            signalConsumer();
        } else {
            printExistRequestWarning(downloadRequest);
        }
    }

    void consumeRequest() {
        waitForConsumer();
        DownloadRequest downloadRequest = requestQueue.poll();
        DownloadTask downloadTask = null;
        if (downloadRequest != null) {
            if (!downloadManager.isTaskRunning(downloadRequest.getId())) {
                downloadTask = createTaskFromRequest(downloadRequest);
            } else {
                printExistRequestWarning(downloadRequest);
            }
        }
        if (downloadTask != null) {
            DownloadTaskExecutor downloadTaskExecutor = downloadTask.getRequest().getDownloadExecutor();
            if (downloadTaskExecutor == null) {
                downloadTaskExecutor = defaultTaskExecutor;
            }
            if (!downloadTaskExecutors.contains(downloadTaskExecutor)) {
                downloadTaskExecutor.init();
                downloadTaskExecutors.add(downloadTaskExecutor);
            }
            downloadTaskExecutor.execute(downloadTask);
        }
    }

    @Override
    public void run() {
        while (isRunnable()) {
            consumeRequest();
        }
    }

    public synchronized void cancel() {
        isCanceled.getAndSet(true);
        signalConsumer();
        downloadTaskExecutors.clear();
        if (defaultTaskExecutor != null) {
            defaultTaskExecutor.shutdown();
        }
    }

    boolean isBlockForConsumeRequest() {
        return requestQueue.isEmpty() && isRunnable();
    }

    void waitForConsumer() {
        lock.lock();
        try {
            while (isBlockForConsumeRequest()) {
                consumer.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            unlockSafely();
        }
    }

    boolean isRunnable() {
        return isAlive() && !isCanceled.get();
    }

    void signalConsumer() {
        lock.lock();
        try {
            consumer.signal();
        } finally {
            unlockSafely();
        }
    }

    private void unlockSafely() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    void printExistRequestWarning(DownloadRequest request) {
        LogUtil.w("task " + request.getName() + " already enqueue,we need do nothing.");
    }

    DownloadTask createTaskFromRequest(DownloadRequest downloadRequest) {
        if (!isUsableSpaceEnough(downloadRequest)) {
            return null;
        }
        String url = downloadRequest.getUrl();
        String id = downloadRequest.getId();
        String tag = downloadRequest.getTag();
        String filePath = downloadRequest.getFilePath();
        Uri schemaUri = downloadRequest.getUri();
        DownloadDetailsInfo downloadInfo = downloadRequest.getDownloadInfo();
        if (downloadInfo == null) {
            downloadInfo = createDownloadInfo(id, url, filePath, tag, schemaUri);
        }
        downloadRequest.setDownloadInfo(downloadInfo);
        downloadInfo.setDownloadRequest(downloadRequest);
        downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
        return new DownloadTask(downloadRequest);
    }

    boolean isUsableSpaceEnough(DownloadRequest downloadRequest) {
        Context context = DownloadProvider.context;
        String filePath = downloadRequest.getFilePath();
        long dataFileUsableSpace = Util.getUsableSpace(context.getFilesDir().getParentFile());
        long minUsableStorageSpace = getMinUsableStorageSpace();
        if (dataFileUsableSpace <= minUsableStorageSpace) {
            String dataFileAvailableSize = Formatter.formatFileSize(context, dataFileUsableSpace);
            LogUtil.e("Data directory usable space [" + dataFileAvailableSize + "] and less than minUsableStorageSpace[" + Formatter.formatFileSize(context, minUsableStorageSpace));
            DownloadDetailsInfo downloadInfo = downloadInfoManager.createDownloadInfo(downloadRequest.getUrl(),
                    filePath, downloadRequest.getTag(), downloadRequest.getId(), System.currentTimeMillis(), downloadRequest.getUri(), false);
            downloadInfo.setErrorCode(ErrorCode.ERROR_USABLE_SPACE_NOT_ENOUGH);
            PumpFactory.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
            return false;
        }
        return true;
    }

    long getMinUsableStorageSpace() {
        return PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
    }

    DownloadDetailsInfo createDownloadInfo(String id, String url, String filePath, String tag, Uri schemaUri) {
        DownloadDetailsInfo downloadInfo = DBService.getInstance().getDownloadInfo(id);
        if (downloadInfo == null) {
            //create a new instance if not found.
            downloadInfo = downloadInfoManager.createDownloadInfo(url, filePath, tag, id, System.currentTimeMillis(), schemaUri);
        }
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }
}