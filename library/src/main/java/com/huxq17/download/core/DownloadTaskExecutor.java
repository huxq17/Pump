package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;

public interface DownloadTaskExecutor {
    /**
     * Do some initialization and it will only be called once.
     */
    void init();

    /**
     * Executes the given task sometime in the future.
     *
     * @param downloadTask Download task
     */
    void execute(DownloadTask downloadTask);

    /**
     * Return the maximum number of download to execute concurrently.
     *
     * @return The maximum number of download to execute concurrently
     */
    int getMaxDownloadNumber();

    /**
     * Return the name of this executor,use for logging.
     *
     * @return The name of this executor,use for logging.
     */
    String getName();

    /**
     * Return the tag of this executor, use for tag all tasks that are executed by current executor,
     * and will override {@link com.huxq17.download.core.DownloadRequest.DownloadGenerator#tag(String)}
     *
     * @return The tag of current executor.
     */
    String getTag();

    /**
     * Terminal this Executor.
     */
    void shutdown();
}
