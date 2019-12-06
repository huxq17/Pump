package com.huxq17.download.config;

import android.content.Context;

import com.huxq17.download.OnVerifyMd5Listener;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.provider.Provider;

public class DownloadConfig {
    /**
     * 允许同时下载的最大任务数量
     */
    private int maxRunningTaskNumber = 3;
    /**
     * 最小可用的内存空间
     */
    private long minUsableStorageSpace = 4 * 1024L;
    private OnVerifyMd5Listener onVerifyMd5Listener;

    private DownloadConfig() {
    }

    public int getMaxRunningTaskNumber() {
        return maxRunningTaskNumber;
    }

    public long getMinUsableSpace() {
        return minUsableStorageSpace;
    }

    public static Builder newBuilder(Context context) {
        Provider.init(context);
        return new Builder();
    }

    public OnVerifyMd5Listener getOnVerifyMd5Listener() {
        return onVerifyMd5Listener;
    }

    public static class Builder {
        private DownloadConfig downloadConfig;

        private Builder() {
            this.downloadConfig = new DownloadConfig();
        }

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
         * @param maxRunningTaskNumber maximum number of tasks to run
         * @return
         */
        public Builder setMaxRunningTaskNum(int maxRunningTaskNumber) {
            downloadConfig.maxRunningTaskNumber = maxRunningTaskNumber;
            return this;
        }

        /**
         * Set the minimum available storage space size for downloading to avoid insufficient storage space during downloading, default is 4kb。
         *
         * @param minUsableStorageSpace minimum available storage space size
         * @return
         */
        public Builder setMinUsableStorageSpace(long minUsableStorageSpace) {
            downloadConfig.minUsableStorageSpace = minUsableStorageSpace;
            return this;
        }

        public Builder setDownloadConnectionFactory(DownloadConnection.Factory factory) {
            PumpFactory.getService(IDownloadConfigService.class).setDownloadConnectionFactory(factory);
            return this;
        }

        public void build() {
            PumpFactory.getService(IDownloadConfigService.class).setConfig(downloadConfig);
        }
    }
}
