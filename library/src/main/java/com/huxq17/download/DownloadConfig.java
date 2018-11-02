package com.huxq17.download;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.manager.IDownloadManager;

public class DownloadConfig {
    /**
     * 下载单个文件时开启的线程数量
     */
    private int downloadThreadNumber;
    /**
     * 允许同时下载的最大任务数量
     */
    private int maxRunningTaskNumber;
    /**
     * 是否重复下载已经下载完成了的文件
     */
    private boolean forceReDownload;

    private DownloadConfig(int downloadThreadNumber, int maxRunningTaskNumber, boolean forceReDownload) {
        this.downloadThreadNumber = downloadThreadNumber;
        this.maxRunningTaskNumber = maxRunningTaskNumber;
        this.forceReDownload = forceReDownload;
    }

    public int getDownloadThreadNumber() {
        return downloadThreadNumber;
    }

    public int getMaxRunningTaskNumber() {
        return maxRunningTaskNumber;
    }

    public boolean isForceReDownload() {
        return forceReDownload;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private int downloadThreadNumber = 3;
        private int maxRunningTaskNumber = 3;
        private boolean forceReDownload = false;

        /**
         * Set how many threads are used when downloading,default 3.
         *
         * @param threadNum
         * @return
         */
        public Builder setThreadNum(int threadNum) {
            downloadThreadNumber = threadNum;
            return this;
        }

        /**
         * Set the maximum number of tasks to run, default 3.
         *
         * @param maxRunningTaskNumber
         * @return
         */
        public Builder setMaxRunningTaskNum(int maxRunningTaskNumber) {
            this.maxRunningTaskNumber = maxRunningTaskNumber;
            return this;
        }

        /**
         * Set whether to repeatedly download the downloaded file,default false.
         *
         * @param forceReDownload
         * @return
         */
        public Builder setForceReDownload(boolean forceReDownload) {
            this.forceReDownload = forceReDownload;
            return this;
        }

        public void build() {
            DownloadConfig config = new DownloadConfig(downloadThreadNumber, maxRunningTaskNumber, forceReDownload);
            ServiceAgency.getService(IDownloadManager.class).setDownloadConfig(config);
        }
    }
}
