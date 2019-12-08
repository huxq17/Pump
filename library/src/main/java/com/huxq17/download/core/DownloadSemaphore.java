package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DownloadSemaphore {
    private AtomicInteger runningTaskCount = new AtomicInteger();
    private ConcurrentLinkedQueue<DownloadTask> waitingTaskQueue = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<DownloadTask> runningTaskQueue = new ConcurrentLinkedQueue<>();

    public final void execute(DownloadTask downloadTask) {
        runningTaskCount.incrementAndGet();
        runningTaskQueue.offer(downloadTask);
    }

    void offer(DownloadTask downloadTask) {
        waitingTaskQueue.offer(downloadTask);
    }

    boolean isWaitingQueueEmpty() {
        return waitingTaskQueue.isEmpty();
    }

    boolean contains(String id) {
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

    void remove(DownloadTask downloadTask) {
        waitingTaskQueue.remove(downloadTask);
        runningTaskQueue.remove(downloadTask);
    }

    DownloadTask poll() {
        return waitingTaskQueue.poll();
    }

    public final void release() {
        runningTaskCount.decrementAndGet();
    }

    public final int availablePermits() {
        return getPermits() - runningTaskCount.get();
    }

    public abstract int getPermits();
}
