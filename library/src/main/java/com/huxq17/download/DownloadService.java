package com.huxq17.download;

import android.content.Context;
import android.os.Environment;
import android.text.format.Formatter;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
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

import static com.huxq17.download.Utils.Util.MIN_STORAGE_USABLE_SPACE;

public class DownloadService implements Task, DownLoadLifeCycleObserver {
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private AtomicBoolean isRunning = new AtomicBoolean();
    private AtomicBoolean isCanceled = new AtomicBoolean();
    private ConcurrentLinkedQueue<DownloadRequest> requestQueue;
    private ConcurrentLinkedQueue<DownloadTask> waitingTaskQueue;
    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;

    private Lock lock = new ReentrantLock();
    private Condition consumer = lock.newCondition();
    /**
     * 正在下载的任务map
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
        if (isRunning.get()) {
            requestQueue.add(request);
            signalConsumer();
        }
    }

    public void setMaxRunningTaskNumber(int maxRunningTaskNumber) {
        this.maxRunningTaskNumber = maxRunningTaskNumber;
    }

    private boolean consumeRequest() {
        lock.lock();
        try {
            if (requestQueue.isEmpty() && waitingTaskQueue.isEmpty()) {
                consumer.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        DownloadRequest downloadRequest = requestQueue.poll();
        if (downloadRequest != null) {
            String url = downloadRequest.getUrl();
            String filePath = downloadRequest.getFilePath();
            String tag = downloadRequest.getTag();
            String id = downloadRequest.getId();

            long downloadDirUsableSpace = Util.getUsableSpace(new File(downloadRequest.getFilePath()));
            long dataFileUsableSpace = Util.getUsableSpace(Environment.getDataDirectory());
            if (downloadDirUsableSpace <= MIN_STORAGE_USABLE_SPACE || dataFileUsableSpace <= MIN_STORAGE_USABLE_SPACE) {
                Context context = PumpFactory.getService(IDownloadManager.class).getContext();
                String dataFileAvailableSize = Formatter.formatFileSize(context, dataFileUsableSpace);
                String downloadFileAvailableSize = Formatter.formatFileSize(context, downloadDirUsableSpace);
                LogUtil.e("data directory usable space is " + dataFileAvailableSize + "and download directory usable space is " + downloadFileAvailableSize);
                DownloadDetailsInfo downloadInfo = new DownloadDetailsInfo(url, filePath, tag, id);
                downloadInfo.setErrorCode(ErrorCode.USABLE_SPACE_NOT_ENOUGH);
                PumpFactory.getService(IMessageCenter.class).notifyProgressChanged(downloadInfo);
                return false;
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
        return true;
    }

    private boolean consumeTask() {
        lock.lock();
        try {
            while (requestQueue.isEmpty() && runningTaskQueue.size() >= maxRunningTaskNumber && isRunning()) {
                consumer.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        if (runningTaskQueue.size() < maxRunningTaskNumber && isRunning()) {
            DownloadTask downloadTask = waitingTaskQueue.poll();
            if (downloadTask != null) {
                LogUtil.d("start run " + downloadTask.getName());
                runningTaskQueue.offer(downloadTask);
                TaskManager.execute(downloadTask);
            }
        }
        return true;
    }

    @Override
    public void run() {
        LogUtil.d("DownloadService start");
        while (isRunning()) {
            consumeRequest();
            consumeTask();
        }
        isRunning.set(false);
        LogUtil.d("DownloadService stopped");
    }


    public boolean isRunning() {
        return isRunning.get() && !isCanceled.get();
    }

    @Override
    public void cancel() {
        isCanceled.set(true);
        signalConsumer();
    }

    private DownloadDetailsInfo createDownloadInfo(String id, String url, String filePath, String tag) {
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

    private void signalConsumer() {
        lock.lock();
        try {
            consumer.signal();
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
}
