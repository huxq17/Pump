package com.huxq17.download.config;

import com.huxq17.download.DownloadConfig;
import com.huxq17.download.OnVerifyMd5Listener;

public interface IDownloadConfigService {
    void setConfig(DownloadConfig downloadConfig);

    int getMaxRunningTaskNumber();

    long getMinUsableSpace();

    OnVerifyMd5Listener getOnVerifyMd5Listener();
}
