package com.huxq17.download.config;

import com.huxq17.download.DownloadConfig;

public interface IDownloadConfigService {
    void setConfig(DownloadConfig downloadConfig);
    int getMaxRunningTaskNumber();

    long getMinUsableSpace();
}
