package com.huxq17.download.core;

import okhttp3.Request;

public interface DownloadInterceptor {
    DownloadInfo intercept(DownloadChain chain);

    interface DownloadChain {
        DownloadRequest request();

        DownloadInfo proceed(DownloadRequest request);
    }
}
