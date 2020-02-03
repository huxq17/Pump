package com.huxq17.download.core;


import android.text.TextUtils;

import com.huxq17.download.Pump;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.DownloadListener;


public final class DownloadRequest {
    private final String id;
    private final String url;
    private final String filePath;
    private final int threadNum;
    private final String tag;
    private final boolean forceReDownload;
    private final int retryCount;
    private final int retryDelay;
    //Maybe use in the future
    private final DownloadListener downloadListener;

    private final DownloadTaskExecutor downloadTaskExecutor;
    private DownloadDetailsInfo downloadInfo;
    private final boolean disableBreakPointDownload;

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
    }

    void setDownloadInfo(DownloadDetailsInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        downloadInfo.setFilePath(filePath);
    }

    public int getRetryDelay() {
        return retryDelay < 0 ? 0 : retryDelay;
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
        return retryCount < 0 ? 0 : retryCount;
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        downloadInfo.setFilePath(filePath);
    }

    public int getThreadNum() {
        return threadNum;
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

    public static DownloadGenerator newRequest(String url, String filePath) {
        return new DownloadGenerator(url, filePath);
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

        public DownloadGenerator(String url, String filePath) {
            this.url = url;
            this.filePath = filePath;
        }

        public DownloadGenerator setId(String id) {
            this.id = id;
            return this;
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
         * @return
         */
        public DownloadGenerator tag(String tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Set whether to repeatedly download the downloaded file,default false.
         *
         * @param force
         * @return
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
         * Set retry count and retry interval.
         * Retry only if the network connection fails.
         *
         * @param retryCount  retry count
         * @param delayMillis The delay (in milliseconds)  until the Retry
         *                    will be executed.The default value is 200 milliseconds.
         * @return
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
                this.downloadListener.enable();
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
            if (getId().equals(downloadRequest.getId())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

}
