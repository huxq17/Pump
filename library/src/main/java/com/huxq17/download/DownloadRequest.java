package com.huxq17.download;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.provider.Provider;

public class DownloadRequest {
    private String url;
    private String filePath;
    private int threadNum = 3;
    private boolean forceReDownload = false;
    private DownloadDetailsInfo downloadInfo;
    private Provider.CacheBean cacheBean;
    private int retryTimes = 0;

    public void setCacheBean(Provider.CacheBean cacheBean) {
        this.cacheBean = cacheBean;
    }

    public Provider.CacheBean getCacheBean() {
        return cacheBean;
    }

    public void setDownloadInfo(DownloadDetailsInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }

    public DownloadDetailsInfo getDownloadInfo() {
        return downloadInfo;
    }

    private DownloadRequest(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public int getRetryTimes() {
        return retryTimes;
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

        public DownloadGenerator setRetryTimes(int retryTimes) {
            downloadRequest.retryTimes = retryTimes;
            return this;
        }

        public void submit() {
            ServiceAgency.getService(IDownloadManager.class).submit(downloadRequest);
        }
    }
}
