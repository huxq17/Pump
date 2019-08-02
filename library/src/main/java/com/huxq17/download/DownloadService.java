package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.task.DownloadTask;
import com.huxq17.download.task.Task;

import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadService implements Task, DownLoadLifeCycleObserver {
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;
    private AtomicBoolean isRunning = new AtomicBoolean();
    private AtomicBoolean isCanceled = new AtomicBoolean();
    private LinkedBlockingQueue<DownloadRequest> requestQueue;
    private LinkedList<DownloadTask> runningTaskQueue;
    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;
    /**
     * 正在下载的任务数量
     */
    private AtomicInteger runningNum = new AtomicInteger();
    private Lock lock = new ReentrantLock();
    private Condition notEmpty = lock.newCondition();
    private Condition notFull = lock.newCondition();
    private DownloadTaskExecutor downloadTaskExecutor;

    public DownloadService(DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
        downloadTaskExecutor =  new DownloadTaskExecutor(this);
    }

    public void start() {
        isRunning.set(true);
        isCanceled.set(false);
        runningTaskQueue = new LinkedList<>();
        requestQueue = new LinkedBlockingQueue<>();
        TaskManager.execute(this);
        downloadTaskExecutor.start();
    }

    public void addDownloadRequest(DownloadRequest request) {
        if (isRunning.get()) {
            requestQueue.add(request);
        }
    }

    public void setMaxRunningTaskNumber(int maxRunningTaskNumber) {
        this.maxRunningTaskNumber = maxRunningTaskNumber;
    }

    @Override
    public void run() {
        LogUtil.d("DownloadService start");
        while (isRunning.get()) {
            try {
                DownloadRequest downloadRequest = requestQueue.take();
                if (isCanceled.get()) {
                    requestQueue.clear();
                    isRunning.set(false);
                    break;
                }
                DownloadDetailsInfo downloadInfo = downloadRequest.getDownloadInfo();
                DownloadTask downloadTask;
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
                downloadTask = new DownloadTask(downloadRequest, this);
                runningTaskQueue.add(downloadTask);
                signalNotEmpty();
                downLoadLifeCycleObserver.onDownloadStart(downloadTask);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        LogUtil.d("DownloadService stopped");
        isRunning.set(false);
    }

    public DownloadTask getDownloadTask() {
        lockIfTaskFull();
        if (!isRunning()) {
            return null;
        }
        DownloadTask downloadTask = lockIfTaskEmpty();
        if (downloadTask != null) {
            runningNum.incrementAndGet();
        }
        return downloadTask;

    }

    public boolean isRunning() {
        return isRunning.get() && !isCanceled.get();
    }

    @Override
    public void cancel() {
        isCanceled.set(true);
        addDownloadRequest(new ShutdownRequest());
        cancelDownloadTaskExecutor();
    }

    private void cancelDownloadTaskExecutor() {
        signalNotFull();
        signalNotEmpty();
    }

    private void signalNotFull() {
        lock.lock();
        notFull.signal();
        lock.unlock();
    }

    private void lockIfTaskFull() {
        lock.lock();
        while (runningNum.get() >= maxRunningTaskNumber && isRunning()) {
            try {
                notFull.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        lock.unlock();
    }

    private void signalNotEmpty() {
        lock.lock();
        notEmpty.signal();
        lock.unlock();
    }

    private DownloadTask lockIfTaskEmpty() {
        lock.lock();
        DownloadTask downloadTask = null;
        try {
            while (runningTaskQueue.isEmpty() && isRunning()) {
                notEmpty.await();
            }
            downloadTask = runningTaskQueue.poll();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return downloadTask;
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
        if (runningNum.get() > 0) {
            runningNum.decrementAndGet();
        }
        signalNotFull();
        downLoadLifeCycleObserver.onDownloadEnd(downloadTask);
        runningTaskQueue.remove(downloadTask);
    }
}
