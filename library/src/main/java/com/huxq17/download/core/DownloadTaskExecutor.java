package com.huxq17.download.core;

public interface DownloadTaskExecutor {
    /**
     * Do some initialization and it will only be called once
     */
    void init();

    /**
     * Executes the given task sometime in the future.
     *
     * @param downloadTask Download task
     */
    void execute(Runnable downloadTask);

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
     * Return the tag of this executor, use for tag all tasks that are executed
     * by current executor,and will cover {@link com.huxq17.download.core.DownloadRequest.DownloadGenerator#tag(String)}
     *
     * @return The tag of current executor.
     */
    String getTag();

    /**
     * Release executor resource.
     */
    void release();
}
