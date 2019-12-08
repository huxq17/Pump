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


public class DownloadService implements Task, DownLoadLifeCycleObserver {
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private AtomicBoolean isRunning = new AtomicBoolean();
    private AtomicBoolean isCanceled = new AtomicBoolean();
    private ConcurrentLinkedQueue<DownloadRequest> requestQueue;

    private Lock lock = new ReentrantLock();
    private Condition consumer = lock.newCondition();
    private HashSet<DownloadSemaphore> semaphoreList = new HashSet<>(1);
    private DownloadSemaphore defaultDownloadSemaphore = new DownloadSemaphore() {

        @Override
        public int getPermits() {
            return PumpFactory.getService(IDownloadConfigService.class).getMaxRunningTaskNumber();
        }
    };

    public DownloadService(DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
    }

    public void start() {
        isRunning.set(true);
        isCanceled.set(false);
        requestQueue = new ConcurrentLinkedQueue<>();
        TaskManager.execute(this);
        semaphoreList.add(defaultDownloadSemaphore);
    }

    public void enqueueRequest(DownloadRequest request) {
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
        if (isBlockForConsumeRequest()) {
            waitForConsumer();
        }
        DownloadRequest downloadRequest = requestQueue.poll();
        if (downloadRequest != null && !taskIsExists(downloadRequest.getId())) {
            String url = downloadRequest.getUrl();
            String filePath = downloadRequest.getFilePath();
            String tag = downloadRequest.getTag();
            String id = downloadRequest.getId();
            if (!isUsableSpaceEnough(downloadRequest)) {
                return;
            }
            DownloadDetailsInfo downloadInfo = downloadRequest.getDownloadInfo();
            if (downloadInfo == null) {
                downloadInfo = createDownloadInfo(id, url, filePath, tag);
                downloadRequest.setDownloadInfo(downloadInfo);
            }
            if (downloadRequest.isForceReDownload() && downloadInfo.isFinished()) {
                downloadInfo.setCompletedSize(0);
            }
            downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
            DownloadSemaphore downloadSemaphore = downloadRequest.getDownloadSemaphore();
            if (downloadSemaphore == null) {
                downloadSemaphore = defaultDownloadSemaphore;
            }
            semaphoreList.add(downloadSemaphore);
            DownloadTask downloadTask = new DownloadTask(downloadRequest, downloadSemaphore, this);
            downloadSemaphore.offer(downloadTask);
            LogUtil.d("Task " + downloadTask.getName() + " is ready.");
            downLoadLifeCycleObserver.onDownloadStart(downloadTask);
        }
    }

    void consumeTask() {
        for (DownloadSemaphore downloadSemaphore : semaphoreList) {
            int availablePermits = downloadSemaphore.availablePermits();
            if (availablePermits > 0 && isRunnable()) {
                DownloadTask downloadTask = downloadSemaphore.poll();
                executeDownloadTask(downloadTask);
            } else if (downloadSemaphore.availablePermits() <= 0) {
                LogUtil.d("Running " + (downloadSemaphore.getPermits() - downloadSemaphore.availablePermits())
                        + " tasks;but max allow run " + downloadSemaphore.getPermits() + " tasks.");
            }
        }
        while (getAvailablePermits() == 0 && requestQueue.isEmpty() && isRunnable()) {
            waitForConsumer();
        }
    }

    int getAvailablePermits() {
        int result = 0;
        for (DownloadSemaphore downloadSemaphore : semaphoreList) {
            result += downloadSemaphore.availablePermits();
        }
        return result;
    }

    void executeDownloadTask(DownloadTask downloadTask) {
        if (downloadTask != null) {
            downloadTask.getDownloadSemaphore().execute(downloadTask);
        }
    }

    @Override
    public void run() {
        while (isRunnable()) {
            consumeRequest();
            consumeTask();
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
    }

    DownloadDetailsInfo createDownloadInfo(String id, String url, String filePath, String tag) {
        DownloadDetailsInfo downloadInfo = DBService.getInstance().getDownloadInfo(id);
        if (downloadInfo != null) {
            return downloadInfo;
        }
        //create a new instance if not found.
        downloadInfo = new DownloadDetailsInfo(url, filePath, tag, id, System.currentTimeMillis());
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    @Override
    public void onDownloadStart(DownloadTask downloadTask) {
    }

    boolean isBlockForConsumeRequest() {
        boolean isWaitQueueEmpty = true;
        for (DownloadSemaphore downloadSemaphore : semaphoreList) {
            if (!downloadSemaphore.isWaitingQueueEmpty()) {
                isWaitQueueEmpty = false;
                break;
            }
        }
        return requestQueue.isEmpty() && isWaitQueueEmpty;
    }

    boolean isBlockForConsumeTask(DownloadSemaphore downloadSemaphore) {
        return requestQueue.isEmpty() && downloadSemaphore.availablePermits() <= 0 && isRunning();
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

    void waitForConsumer() {
        lock.lock();
        try {
            consumer.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onDownloadEnd(DownloadTask downloadTask) {
        for (DownloadSemaphore downloadSemaphore : semaphoreList) {
            downloadSemaphore.remove(downloadTask);
        }
        downLoadLifeCycleObserver.onDownloadEnd(downloadTask);
        signalConsumer();
    }

    void printExistRequestWarning(DownloadRequest request) {
        LogUtil.e("task " + request.getName() + " already enqueue,we need do nothing.");
    }

    boolean taskIsExists(String id) {
        boolean exists = false;
        for (DownloadSemaphore downloadSemaphore : semaphoreList) {
            if (downloadSemaphore.contains(id)) {
                exists = true;
                break;
            }
        }
        return exists;
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
            DownloadDetailsInfo downloadInfo = new DownloadDetailsInfo(downloadRequest.getUrl(), filePath, downloadRequest.getTag(), downloadRequest.getId(), System.currentTimeMillis());
            downloadInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
            PumpFactory.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
            return false;
        }
        return true;
    }

    long getMinUsableStorageSpace() {
        return PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
    }
}
