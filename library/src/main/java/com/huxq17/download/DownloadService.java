package com.huxq17.download;

import android.content.Context;
import android.os.Environment;
import android.text.format.Formatter;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.IMessageCenter;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

import java.io.File;
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
    private ConcurrentLinkedQueue<DownloadTask> waitingTaskQueue;

    private Lock lock = new ReentrantLock();
    private Condition consumer = lock.newCondition();
    /**
     * 正在下载的任务
     */
    private ConcurrentLinkedQueue<DownloadTask> runningTaskQueue;

    public DownloadService(DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
    }

    public void start() {
        isRunning.set(true);
        isCanceled.set(false);
        waitingTaskQueue = new ConcurrentLinkedQueue<>();
        requestQueue = new ConcurrentLinkedQueue<>();
        runningTaskQueue = new ConcurrentLinkedQueue<>();
        TaskManager.execute(this);
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
            DownloadTask downloadTask = new DownloadTask(downloadRequest, this);
            waitingTaskQueue.offer(downloadTask);
            LogUtil.d("Task " + downloadTask.getName() + " is ready.");
            downLoadLifeCycleObserver.onDownloadStart(downloadTask);
        }
    }

    void consumeTask() {
        while (isBlockForConsumeTask()) {
            LogUtil.d("running " + runningTaskQueue.size() + " tasks;but max allow run " + getMaxRunningTaskNumber() + " tasks.");
            waitForConsumer();
        }
        if (runningTaskQueue.size() < getMaxRunningTaskNumber() && isRunning()) {
            DownloadTask downloadTask = waitingTaskQueue.poll();
            if (downloadTask != null) {
                LogUtil.d("start run " + downloadTask.getName());
                runningTaskQueue.offer(downloadTask);
                executeDownloadTask(downloadTask);
            }
        }
    }

    void executeDownloadTask(DownloadTask downloadTask) {
        TaskManager.execute(downloadTask);
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
        downloadInfo = new DownloadDetailsInfo(url, filePath, tag, id);
        downloadInfo.setCreateTime(System.currentTimeMillis());
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    @Override
    public void onDownloadStart(DownloadTask downloadTask) {
    }

    boolean isBlockForConsumeRequest() {
        return requestQueue.isEmpty() && waitingTaskQueue.isEmpty();
    }

    boolean isBlockForConsumeTask() {
        return requestQueue.isEmpty() && runningTaskQueue.size() >= getMaxRunningTaskNumber() && isRunning();
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
        runningTaskQueue.remove(downloadTask);
        waitingTaskQueue.remove(downloadTask);
        downLoadLifeCycleObserver.onDownloadEnd(downloadTask);
        signalConsumer();
    }

    void printExistRequestWarning(DownloadRequest request) {
        LogUtil.e("task " + request.getName() + " already enqueue,we need do nothing.");
    }

    boolean taskIsExists(String id) {
        boolean exists = false;
        for (DownloadTask task : waitingTaskQueue) {
            if (task.getId().equals(id)) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            for (DownloadTask task : runningTaskQueue) {
                if (task.getId().equals(id)) {
                    exists = true;
                    break;
                }
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
            DownloadDetailsInfo downloadInfo = new DownloadDetailsInfo(downloadRequest.getUrl(), filePath, downloadRequest.getTag(), downloadRequest.getId());
            downloadInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
            PumpFactory.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
            return false;
        }
        return true;
    }

    int getMaxRunningTaskNumber() {
        return PumpFactory.getService(IDownloadConfigService.class).getMaxRunningTaskNumber();
    }

    long getMinUsableStorageSpace() {
        return PumpFactory.getService(IDownloadConfigService.class).getMinUsableSpace();
    }
}
