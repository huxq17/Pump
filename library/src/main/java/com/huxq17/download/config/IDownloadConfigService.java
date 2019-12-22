package com.huxq17.download.config;

import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.connection.DownloadConnection;

import java.util.List;

public interface IDownloadConfigService {
    void setConfig(DownloadConfig downloadConfig);

    int getMaxRunningTaskNumber();

    long getMinUsableSpace();

    List<DownloadInterceptor> getDownloadInterceptors();

    void setDownloadConnectionFactory(DownloadConnection.Factory factory);

    DownloadConnection.Factory getDownloadConnectionFactory();
}
