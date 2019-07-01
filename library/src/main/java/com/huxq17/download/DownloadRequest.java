package com.huxq17.download;


import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.DownloadListener;
import com.huxq17.download.provider.Provider;

public class DownloadRequest {
    private String url;
    private String filePath;
    private int threadNum = 3;
    private String tag;
    private boolean forceReDownload = false;
    private DownloadDetailsInfo downloadInfo;
    private Provider.CacheBean cacheBean;
    private int retryCount = 0;
    private static final int DEFAULT_RETRY_DELAY = 200;
    private int retryDelay = DEFAULT_RETRY_DELAY;

    public void setCacheBean(Provider.CacheBean cacheBean) {
        this.cacheBean = cacheBean;
    }

    public Provider.CacheBean getCacheBean() {
        return cacheBean;
    }

    public void setDownloadInfo(DownloadDetailsInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public DownloadDetailsInfo getDownloadInfo() {
        return downloadInfo;
    }

    private DownloadRequest(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public String getUrl() {
        return url;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getThreadNum() {
        return threadNum;
    }

    public String getTag() {
        return tag == null ? "" : tag;
    }

    public void setThreadNum(int threadNum) {
        this.threadNum = threadNum;
    }

    public boolean isForceReDownload() {
        return forceReDownload;
    }

    public static DownloadGenerator newRequest(String url, String filePath) {
        return new DownloadGenerator(url, filePath);
    }

    public static class DownloadGenerator {
        private DownloadRequest downloadRequest;

        public DownloadGenerator(String url, String filePath) {
            downloadRequest = new DownloadRequest(url, filePath);
        }

        public DownloadGenerator threadNum(int threadNum) {
            downloadRequest.threadNum = threadNum;
            return this;
        }

        public DownloadGenerator listener(DownloadListener listener) {
            listener.enable();
            return this;
        }

        public DownloadGenerator tag(String tag) {
            downloadRequest.tag = tag;
            return this;
        }

        /**
         * Set whether to repeatedly download the downloaded file,default false.
         *
         * @param force
         * @return
         */
        public DownloadGenerator forceReDownload(boolean force) {
            downloadRequest.forceReDownload = force;
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
            downloadRequest.retryCount = retryCount;
            if (delayMillis < 0) {
                delayMillis = DEFAULT_RETRY_DELAY;
            }
            downloadRequest.retryDelay = delayMillis;
            return this;
        }

        public DownloadGenerator setRetry(int retryCount) {
            setRetry(retryCount, -1);
            return this;
        }

        public void submit() {
            PumpFactory.getService(IDownloadManager.class).submit(downloadRequest);
        }
    }
}
