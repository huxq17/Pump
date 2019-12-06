package com.huxq17.download.config;

import com.huxq17.download.OnVerifyMd5Listener;
import com.huxq17.download.core.connection.DownloadConnection;

public interface IDownloadConfigService {
    void setConfig(DownloadConfig downloadConfig);

    int getMaxRunningTaskNumber();

    long getMinUsableSpace();

    OnVerifyMd5Listener getOnVerifyMd5Listener();

    void setDownloadConnectionFactory(DownloadConnection.Factory factory);

    DownloadConnection.Factory getDownloadConnectionFactory();
}
