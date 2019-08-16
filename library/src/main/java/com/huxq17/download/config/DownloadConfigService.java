package com.huxq17.download.config;


import com.huxq17.download.DownloadConfig;

public class DownloadConfigService implements IDownloadConfigService {
    /**
     * 允许同时下载的最大任务数量
     */
    private int maxRunningTaskNumber;
    /**
     * 最小可用的内存空间
     */
    private long minUsableStorageSpace = 4 * 1024L;
    private DownloadConfig downloadConfig;

    private DownloadConfigService() {
    }

    @Override
    public void setConfig(DownloadConfig downloadConfig) {
        this.downloadConfig = downloadConfig;
    }

    public int getMaxRunningTaskNumber() {
        return downloadConfig.getMaxRunningTaskNumber();
    }

    public long getMinUsableSpace() {
        return downloadConfig.getMinUsableSpace();
    }
}
