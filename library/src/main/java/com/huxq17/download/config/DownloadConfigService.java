package com.huxq17.download.config;


import com.huxq17.download.DownloadConfig;
import com.huxq17.download.OnVerifyMd5Listener;

public class DownloadConfigService implements IDownloadConfigService {
    /**
     * 允许同时下载的最大任务数量
     */
    private int maxRunningTaskNumber = 3;
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
        if (downloadConfig == null) {
            return maxRunningTaskNumber;
        }
        return downloadConfig.getMaxRunningTaskNumber();
    }

    public long getMinUsableSpace() {
        if (downloadConfig == null) {
            return minUsableStorageSpace;
        }
        return downloadConfig.getMinUsableSpace();
    }

    @Override
    public OnVerifyMd5Listener getOnVerifyMd5Listener() {
        if (downloadConfig == null) {
            return null;
        }
        return downloadConfig.getOnVerifyMd5Listener();
    }
}
