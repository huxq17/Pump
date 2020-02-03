package com.huxq17.download.config;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.utils.OKHttpUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadConfig {
    /**
     * 允许同时下载的最大任务数量
     */
    private int maxRunningTaskNumber = 3;
    /**
     * 最小可用的内存空间
     */
    private long minUsableStorageSpace = 4 * 1024L;

    private DownloadConnection.Factory connectionFactory;
    private List<DownloadInterceptor> interceptors = new ArrayList<>();

    private DownloadConfig() {
    }

    public int getMaxRunningTaskNumber() {
        return maxRunningTaskNumber;
    }

    public long getMinUsableSpace() {
        return minUsableStorageSpace;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<DownloadInterceptor> getInterceptors() {
        return Collections.unmodifiableList(interceptors);
    }

    public DownloadConnection.Factory getDownloadConnectionFactory() {
        return connectionFactory == null ? new OkHttpDownloadConnection.Factory(OKHttpUtil.get())
                : connectionFactory;
    }

    public static class Builder {
        private DownloadConfig downloadConfig;

        private Builder() {
            this.downloadConfig = new DownloadConfig();
        }

        /**
         * Set the maximum number of tasks to run, default 3.
         *
         * @param maxRunningTaskNumber maximum number of tasks to run
         */
        public Builder setMaxRunningTaskNum(int maxRunningTaskNumber) {
            downloadConfig.maxRunningTaskNumber = maxRunningTaskNumber;
            return this;
        }

        /**
         * Set the minimum available storage space size for downloading to avoid insufficient
         * storage space during downloading, default is 4kb.
         *
         * @param minUsableStorageSpace minimum available storage space size
         */
        public Builder setMinUsableStorageSpace(long minUsableStorageSpace) {
            downloadConfig.minUsableStorageSpace = minUsableStorageSpace;
            return this;
        }

        public Builder addDownloadInterceptor(DownloadInterceptor interceptor) {
            downloadConfig.interceptors.add(interceptor);
            return this;
        }

        public Builder setDownloadConnectionFactory(DownloadConnection.Factory factory) {
            downloadConfig.connectionFactory = factory;
            return this;
        }

        public void build() {
            PumpFactory.getService(IDownloadConfigService.class).setConfig(downloadConfig);
        }
    }
}
