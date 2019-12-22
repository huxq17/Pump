package com.huxq17.download.core;

import com.huxq17.download.core.task.DownloadTask;

import java.util.List;

public final class RealDownloadChain implements DownloadInterceptor.DownloadChain {
    private final int index;
    private int calls;
    private final List<DownloadInterceptor> interceptors;
    private final DownloadRequest downloadRequest;

    public RealDownloadChain(List<DownloadInterceptor> interceptors, DownloadRequest downloadRequest
            , int index) {
        this.index = index;
        this.downloadRequest = downloadRequest;
        this.interceptors = interceptors;
    }

    @Override
    public DownloadRequest request() {
        return downloadRequest;
    }

    public DownloadTask downloadTask() {
        return null;
    }

    @Override
    public DownloadInfo proceed(DownloadRequest downloadRequest) {
        return proceed(downloadRequest, false);
    }

    public DownloadInfo proceed(DownloadRequest downloadRequest, boolean shouldRetry) {
        calls++;
        if (!shouldRetry && calls > 1) {
            throw new IllegalStateException("download interceptor " + interceptors.get(index - 1)
                    + " must call proceed() exactly once");
        }
        DownloadInterceptor interceptor = interceptors.get(index);
        DownloadInterceptor.DownloadChain nextChain = new RealDownloadChain(interceptors,
                downloadRequest, index + 1);
        return interceptor.intercept(nextChain);
    }
}
