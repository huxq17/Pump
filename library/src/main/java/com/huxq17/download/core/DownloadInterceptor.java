package com.huxq17.download.core;

public interface DownloadInterceptor {
    DownloadInfo intercept(DownloadChain chain);

    interface DownloadChain {
        DownloadRequest request();

        DownloadInfo proceed(DownloadRequest request);
    }
}
