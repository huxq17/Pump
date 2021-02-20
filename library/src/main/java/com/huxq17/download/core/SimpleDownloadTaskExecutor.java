package com.huxq17.download.core;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.utils.LogUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleDownloadTaskExecutor extends ThreadPoolExecutor implements DownloadTaskExecutor {
    private static final int DEFAULT_THREAD_COUNT = 3;
    private ConcurrentHashMap<String, Long> countTimeMap = new ConcurrentHashMap<>();

    public SimpleDownloadTaskExecutor() {
        super(DEFAULT_THREAD_COUNT, DEFAULT_THREAD_COUNT, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), new DownloadRejectedExecutionHandler());
        allowCoreThreadTimeOut(true);
    }

    @Override
    public void init() {
        setCorePoolSize(getSafeThreadCount());
        setMaximumPoolSize(getSafeThreadCount());
        setThreadFactory(new DownloadDisPatcherThreadFactory());
    }

    public void execute(DownloadTask downloadTask) {
        if (downloadTask == null) {
            throw new NullPointerException();
        }
        super.execute(downloadTask);
        LogUtil.d("Task " + downloadTask.getDownloadName() + " is ready.");
        if (getQueue().size() + getActiveCount() > getMaxDownloadNumber()) {
            String printName = getSafeName();
            LogUtil.w(printName + " only " + getMaxDownloadNumber()
                    + " tasks can be run at the same time;but " + getActiveCount()
                    + " tasks have been run,so " + downloadTask.getDownloadName() + " is waiting.");
        }
    }

    private int getSafeThreadCount() {
        return getMaxDownloadNumber() <= 0 ? DEFAULT_THREAD_COUNT : getMaxDownloadNumber();
    }

    private String getSafeName() {
        String name = getName();
        return name != null && name.length() > 0 ? name : toString();
    }

    private void checkIsDownloadTask(Runnable runnable) {
        if (!(runnable instanceof DownloadTask)) {
            throw new IllegalArgumentException("Only DownloadTask Can be executed.but execute " + runnable.getClass().getCanonicalName());
        }
    }

    protected final void beforeExecute(Thread t, Runnable r) {
        checkIsDownloadTask(r);
        DownloadTask downloadTask = (DownloadTask) r;
        LogUtil.d("start run " + downloadTask.getDownloadName() + " at thread name=" + t.getName());
        countTimeMap.put(downloadTask.getDownloadId(), System.currentTimeMillis());
    }

    @Override
    protected final void afterExecute(Runnable r, Throwable t) {
        checkIsDownloadTask(r);
        DownloadTask downloadTask = (DownloadTask) r;
        Long startTime = countTimeMap.remove(downloadTask.getDownloadId());
        if (startTime != null) {
            LogUtil.d("download " + downloadTask.getDownloadName() + " is stopped,and spend=" + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    public int getMaxDownloadNumber() {
        return PumpFactory.getService(IDownloadConfigService.class).getMaxRunningTaskNumber();
    }

    @Override
    public String getName() {
        return "SimpleDownloadTaskExecutor";
    }

    @Override
    public String getTag() {
        return null;
    }

    public void shutdown() {
        shutdownNow();
    }

    private class DownloadDisPatcherThreadFactory implements ThreadFactory {
        private AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            String threadName = getSafeName() + "-thread-" + count.addAndGet(1);
            t.setName(threadName);
            return t;
        }
    }

    private static class DownloadRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (executor.isShutdown()) return;
            executor.getQueue().offer(r);
        }
    }
}