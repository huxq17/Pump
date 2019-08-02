package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentLinkedQueue<DownloadTask> taskQueue;
    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;

    private Lock lock = new ReentrantLock();
    private Condition notFull = lock.newCondition();
    /**
     * 正在下载的任务map
     */
    private ConcurrentHashMap<String, DownloadTask> runningTaskMap;

    public DownloadService(DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
    }

    public void start() {
        isRunning.set(true);
        isCanceled.set(false);
        taskQueue = new ConcurrentLinkedQueue<>();
        requestQueue = new ConcurrentLinkedQueue<>();
        runningTaskMap = new ConcurrentHashMap<>();
        TaskManager.execute(this);
//        downloadTaskExecutor.start();
    }

    public void addDownloadRequest(DownloadRequest request) {
        if (isRunning.get()) {
            requestQueue.add(request);
            lock.lock();
            try {
                notFull.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public void setMaxRunningTaskNumber(int maxRunningTaskNumber) {
        this.maxRunningTaskNumber = maxRunningTaskNumber;
    }

    private boolean consumeRequest() {
        lock.lock();
        try {
            if (requestQueue.size() == 0 && taskQueue.size() == 0) {
                notFull.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        DownloadRequest downloadRequest = requestQueue.poll();
        if (downloadRequest != null) {
            if (isCanceled.get()) {
                isRunning.set(false);
                return false;
            }
            DownloadDetailsInfo downloadInfo = downloadRequest.getDownloadInfo();
            if (downloadInfo == null) {
                String url = downloadRequest.getUrl();
                String filePath = downloadRequest.getFilePath();
                String tag = downloadRequest.getTag();
                String id = downloadRequest.getId();
                downloadInfo = createDownloadInfo(id, url, filePath, tag);
                downloadRequest.setDownloadInfo(downloadInfo);
            }
            if (downloadRequest.isForceReDownload() && downloadInfo.isFinished()) {
                downloadInfo.setCompletedSize(0);
            }
            downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
            DownloadTask downloadTask = new DownloadTask(downloadRequest, this);
            lock.lock();
            try {
                taskQueue.add(downloadTask);
            } finally {
                lock.unlock();
            }
            LogUtil.d("Task " + downloadTask.getName() + " is ready.");
            downLoadLifeCycleObserver.onDownloadStart(downloadTask);
        }
        return true;
    }

    private boolean consumeTask() {
        lock.lock();
        try {
            while (requestQueue.size() == 0 && runningTaskMap.size() >= maxRunningTaskNumber && isRunning()) {
                notFull.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        if (runningTaskMap.size() < maxRunningTaskNumber && isRunning()) {
            DownloadTask downloadTask = taskQueue.poll();
            if (downloadTask != null) {
                LogUtil.d("start run " + downloadTask.getName());
                runningTaskMap.put(downloadTask.getId(), downloadTask);
                TaskManager.execute(downloadTask);
            }
        }
        if (isCanceled.get()) {
            isRunning.set(false);
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        LogUtil.d("DownloadService start");
        while (consumeRequest() && consumeTask()) {}
        LogUtil.d("DownloadService stopped");
    }


    public boolean isRunning() {
        return isRunning.get() && !isCanceled.get();
    }

    @Override
    public void cancel() {
        isCanceled.set(true);
        lock.lock();
        try {
            notFull.signal();
        } finally {
            lock.unlock();
        }
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

    @Override
    public void onDownloadEnd(DownloadTask downloadTask) {
        runningTaskMap.remove(downloadTask.getId());
        downLoadLifeCycleObserver.onDownloadEnd(downloadTask);
        taskQueue.remove(downloadTask);
        lock.lock();
        try {
            notFull.signal();
        } finally {
            lock.unlock();
        }
    }
}
