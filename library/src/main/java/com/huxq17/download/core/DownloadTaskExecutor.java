package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class DownloadTaskExecutor<Task extends DownloadTask> extends ThreadPoolExecutor {
    private static final int DEFAULT_THREAD_COUNT = 3;
    public DownloadTaskExecutor(ThreadFactory threadFactory) {
        super(DEFAULT_THREAD_COUNT, DEFAULT_THREAD_COUNT, 0, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), threadFactory, new CustomRejectedExecutionHandler());
    }
    public void setThreadCount(int threadCount) {
        setCorePoolSize(threadCount);
        setMaximumPoolSize(threadCount);
    }
    public void execute(Task command) {
        super.execute(command);
    }

    protected final void beforeExecute(Thread t, Runnable r) {
        onExecuteStart((Task) r);
    }

    @Override
    protected final void afterExecute(Runnable r, Throwable t) {
        onExecuteEnd((Task) r);
    }

    protected abstract void onExecuteStart(Task task);

    protected abstract void onExecuteEnd(Task task);

    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) return;
            executor.getQueue().offer(r);
        }
    }
}