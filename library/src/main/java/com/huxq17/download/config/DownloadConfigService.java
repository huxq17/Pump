package com.huxq17.download.config;


import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;
import com.huxq17.download.utils.OKHttpUtil;

import java.util.ArrayList;
import java.util.List;

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
    private DownloadConnection.Factory downloadConnectionFactory;

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
    public void setDownloadConnectionFactory(DownloadConnection.Factory factory) {
        this.downloadConnectionFactory = factory;
    }


    public List<DownloadInterceptor> getDownloadInterceptors() {
        if (downloadConfig == null) {
            return new ArrayList<>();
        }
        return downloadConfig.getInterceptors();
    }

    @Override
    public DownloadConnection.Factory getDownloadConnectionFactory() {
        if (downloadConnectionFactory == null) {
            downloadConnectionFactory = new OkHttpDownloadConnection.Factory(OKHttpUtil.get());
        }
        return downloadConnectionFactory;
    }
}
