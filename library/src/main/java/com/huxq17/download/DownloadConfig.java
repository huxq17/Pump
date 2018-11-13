package com.huxq17.download;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.manager.IDownloadManager;

public class DownloadConfig {
    /**
     * 允许同时下载的最大任务数量
     */
    private int maxRunningTaskNumber;

    private DownloadConfig(int maxRunningTaskNumber) {
        this.maxRunningTaskNumber = maxRunningTaskNumber;
    }


    public int getMaxRunningTaskNumber() {
        return maxRunningTaskNumber;
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private int maxRunningTaskNumber = 3;

        /**
         * Set how many threads are used when downloading,default 3.
         *
         * @param threadNum
         * @return
         */
        @Deprecated
        public Builder setThreadNum(int threadNum) {
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
        @Deprecated
        public Builder setForceReDownload(boolean forceReDownload) {
            return this;
        }

        public void build() {
            DownloadConfig config = new DownloadConfig(maxRunningTaskNumber);
            ServiceAgency.getService(IDownloadManager.class).setDownloadConfig(config);
        }
    }
}
