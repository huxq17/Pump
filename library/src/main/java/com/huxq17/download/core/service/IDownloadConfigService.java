package com.huxq17.download.core.service;

import com.huxq17.download.config.DownloadConfig;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.connection.DownloadConnection;

import java.util.List;

public interface IDownloadConfigService {
    void setConfig(DownloadConfig downloadConfig);

    int getMaxRunningTaskNumber();

    long getMinUsableSpace();

    List<DownloadInterceptor> getDownloadInterceptors();

    DownloadConnection.Factory getDownloadConnectionFactory();
}
