package com.huxq17.download.core;


import android.net.Uri;
import android.text.TextUtils;

import com.huxq17.download.Pump;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.utils.LogUtil;

import java.io.File;

import okhttp3.Request;


public final class DownloadRequest {
    private final String id;
    private final String url;
    private String filePath;
    private final int threadNum;
    private final String tag;
    private final boolean forceReDownload;
    private final int retryCount;
    private final int retryDelay;
    //Maybe use in the future
    private final DownloadListener downloadListener;
    private final DownloadTaskExecutor downloadTaskExecutor;
    private final boolean disableBreakPointDownload;
    private final Request.Builder httpRequestBuilder;

    private DownloadDetailsInfo downloadInfo;

    private final Uri uri;


    DownloadRequest(DownloadGenerator downloadGenerator) {
        this.id = downloadGenerator.id;
        this.url = downloadGenerator.url;
        this.filePath = downloadGenerator.filePath;
        this.threadNum = downloadGenerator.threadNum;
        this.tag = downloadGenerator.tag;
        this.forceReDownload = downloadGenerator.forceReDownload;
        this.retryCount = downloadGenerator.retryCount;
        this.retryDelay = downloadGenerator.retryDelay;
        this.downloadListener = downloadGenerator.downloadListener;
        this.downloadTaskExecutor = downloadGenerator.downloadTaskExecutor;
        this.disableBreakPointDownload = downloadGenerator.disableBreakPointDownload;
        this.httpRequestBuilder = downloadGenerator.httpRequestBuilder;
        if (httpRequestBuilder != null) {
            httpRequestBuilder.url(url);
        }
        this.uri = downloadGenerator.uri;
    }

    public Uri getUri() {
        return uri;
    }

    void setDownloadInfo(DownloadDetailsInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public int getRetryDelay() {
        return Math.max(retryDelay, 0);
    }

    public DownloadDetailsInfo getDownloadInfo() {
        return downloadInfo;
    }

    public String getId() {
        return id == null ? url : id;
    }

    public String getName() {
        return getId();
    }

    public int getRetryCount() {
        return Math.max(retryCount, 0);
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getThreadNum() {
        return Math.max(threadNum, 1);
    }

    public String getTag() {
        if (downloadTaskExecutor != null) {
            String tag = downloadTaskExecutor.getTag();
            if (tag != null && tag.length() > 0) {
                return tag;
            }
        }
        return tag == null ? "" : tag;
    }


    public boolean isForceReDownload() {
        return forceReDownload;
    }

    public boolean isDisableBreakPointDownload() {
        return disableBreakPointDownload;
    }

    public DownloadTaskExecutor getDownloadExecutor() {
        return downloadTaskExecutor;
    }

    public Request.Builder getHttpRequestBuilder() {
        if (httpRequestBuilder == null) {
            return new Request.Builder().url(url).build().newBuilder();
        }
        return httpRequestBuilder.build().newBuilder();
    }

    public static DownloadGenerator newRequest(String url, String filePath, Uri uri) {
        return new DownloadGenerator(url, filePath, uri);
    }

    public static class DownloadGenerator {
        private String id;
        private String url;
        private String filePath;
        private int threadNum;
        private String tag;
        private boolean forceReDownload;
        private int retryCount;
        private int retryDelay;
        private DownloadListener downloadListener;

        private static final int DEFAULT_RETRY_DELAY = 200;
        private DownloadTaskExecutor downloadTaskExecutor;
        private boolean disableBreakPointDownload;
        private Request.Builder httpRequestBuilder;
        private final Uri uri;

        public DownloadGenerator(String url, String filePath, Uri uri) {
            this.url = url;
            this.filePath = filePath;
            this.uri = uri;
        }

        public DownloadGenerator setId(String id) {
            this.id = id;
            return this;
        }

        public Uri getUri() {
            return uri;
        }

        public DownloadGenerator threadNum(int threadNum) {
            this.threadNum = threadNum;
            return this;
        }

        public DownloadGenerator listener(final DownloadListener listener) {
            this.downloadListener = listener;
            return this;
        }


        /**
         * Tag download task, can use {@link Pump#getDownloadListByTag(String)} to get download list
         * filter by tag,and use {@link DownloadInfo#getTag()} to get tag.
         *
         * @param tag tag
         */
        public DownloadGenerator tag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Set whether to repeatedly download the downloaded file,default false.
         *
         * @param force true will re-download if have download completed before.
         */
        public DownloadGenerator forceReDownload(boolean force) {
            this.forceReDownload = force;
            return this;
        }

        public DownloadGenerator disableBreakPointDownload() {
            this.disableBreakPointDownload = true;
            threadNum = 1;
            return this;
        }

        /**
         * Pump will connect server by this OKHttp request builder,so you can customize download's http request.
         * For example,you can specify http method, head and params.
         * If http method isn't GET,will use {@link DownloadGenerator#disableBreakPointDownload()} to improve download speed.
         *
         * @param httpRequestBuilder OKHttp request builder
         */
        public DownloadGenerator setRequestBuilder(Request.Builder httpRequestBuilder) {
            this.httpRequestBuilder = httpRequestBuilder;
            return this;
        }

        /**
         * Set retry count and retry interval.
         * Retry only if the network connection fails.
         *
         * @param retryCount  retry count
         * @param delayMillis The delay (in milliseconds)  until the Retry
         *                    will be executed.The default value is 200 milliseconds.
         */
        public DownloadGenerator setRetry(int retryCount, int delayMillis) {
            if (retryCount < 0) {
                retryCount = 0;
            }
            this.retryCount = retryCount;
            if (delayMillis < 0) {
                delayMillis = DEFAULT_RETRY_DELAY;
            }
            this.retryDelay = delayMillis;
            return this;
        }

        public DownloadGenerator setRetry(int retryCount) {
            setRetry(retryCount, -1);
            return this;
        }

        public DownloadGenerator setDownloadTaskExecutor(DownloadTaskExecutor downloadTaskExecutor) {
            this.downloadTaskExecutor = downloadTaskExecutor;
            return this;
        }

        public void submit() {
            id = TextUtils.isEmpty(this.id) ? url : this.id;
            if (threadNum <= 0) {
                threadNum = 3;
            }
            if (this.downloadListener != null) {
                downloadListener.setId(id);
                downloadListener.enable();
            }
            if (httpRequestBuilder != null &&
                    !"GET".equalsIgnoreCase(httpRequestBuilder.url(url).build().method())) {
                disableBreakPointDownload();
            }
            PumpFactory.getService(IDownloadManager.class).
                    submit(new DownloadRequest(this));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof DownloadRequest) {
            DownloadRequest downloadRequest = (DownloadRequest) obj;
            return getId().equals(downloadRequest.getId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
