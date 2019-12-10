package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.utils.LogUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class DownloadDispatcher {

    private DownloadTaskExecutor<DownloadTask> pool = new DownloadTaskExecutor<DownloadTask>(new DownloadDisPatcherThreadFactory()) {
        ConcurrentHashMap<String, Long> times = new ConcurrentHashMap<>();

        @Override
        protected void onExecuteStart(DownloadTask task) {
            times.put(task.getId(), System.currentTimeMillis());
            LogUtil.d("start run " + task.getName());
        }

        @Override
        protected void onExecuteEnd(DownloadTask task) {
            downLoadLifeCycleObserver.onDownloadEnd(task);
            Long startTime = times.remove(task.getId());
            if (startTime != null) {
                LogUtil.d("download " + task.getName() + " spend=" + (System.currentTimeMillis() - startTime));
            }
        }
    };
    private DownLoadLifeCycleObserver downLoadLifeCycleObserver;

    void setDownLoadLifeCycleObserver(DownLoadLifeCycleObserver downLoadLifeCycleObserver) {
        this.downLoadLifeCycleObserver = downLoadLifeCycleObserver;
        pool.setThreadCount(getMaxDownloadNumber());
    }

    void execute(DownloadTask downloadTask) {
        if (downloadTask != null) {
            pool.execute(downloadTask);
            if (pool.getQueue().size() + pool.getActiveCount() > getMaxDownloadNumber()) {
                String printName = getSafeName();
                LogUtil.w(printName + " only " + getMaxDownloadNumber()
                        + " tasks can be run at the same time;but " + pool.getActiveCount()
                        + " tasks have been run,so " + downloadTask.getName() + " is waiting.");
            }
        }
    }

    public void destory() {
        pool.shutdownNow();
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

    private String getSafeName() {
        String name = getName();
        return name != null && name.length() > 0 ? name : toString();
    }

    /**
     * Return the maximum number of download to execute concurrently.
     *
     * @return The maximum number of download to execute concurrently
     */
    public abstract int getMaxDownloadNumber();

    /**
     * Return the name of this dispatcher,use for logging.
     *
     * @return The name of this dispatcher,use for logging.
     */
    public abstract String getName();

}
