package com.huxq17.download;


import android.text.TextUtils;

import com.huxq17.download.Utils.Util;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.DownloadListener;
import com.huxq17.download.provider.Provider;

import java.io.File;


public class DownloadRequest {
    private String id;
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
    private String md5;
    private OnVerifyMd5Listener onVerifyMd5Listener;
    private OnDownloadSuccessListener onDownloadSuccessListener;

    public String getMd5() {
        return md5 == null ? "" : md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public OnVerifyMd5Listener getOnVerifyMd5Listener() {
        return onVerifyMd5Listener;
    }

    public OnDownloadSuccessListener getOnDownloadSuccessListener() {
        return onDownloadSuccessListener;
    }

    public void setCacheBean(Provider.CacheBean cacheBean) {
        this.cacheBean = cacheBean;
    }

    public Provider.CacheBean getCacheBean() {
        return cacheBean;
    }

    public void setDownloadInfo(DownloadDetailsInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        downloadInfo.setFilePath(filePath);
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public DownloadDetailsInfo getDownloadInfo() {
        return downloadInfo;
    }

    DownloadRequest(String url, String filePath) {
        this.url = url;
        this.filePath = filePath;
    }

    public String getId() {
        return TextUtils.isEmpty(id) ? url : id;
    }

    public String getName() {
        return getId();
    }

    public void setId(String id) {
        this.id = id;
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

    public void setFilePath(String filePath) {
        this.filePath = filePath;
        downloadInfo.setFilePath(filePath);
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

        public DownloadGenerator setId(String id) {
            downloadRequest.id = id;
            return this;
        }

        public DownloadGenerator threadNum(int threadNum) {
            downloadRequest.threadNum = threadNum;
            return this;
        }

        public DownloadGenerator listener(final DownloadListener listener) {
            listener.enable(downloadRequest.getId());
            return this;
        }

        public DownloadGenerator setOnVerifyMd5Listener(OnVerifyMd5Listener listener) {
            downloadRequest.onVerifyMd5Listener = listener;
            return this;
        }

        /**
         * 设置下载成功的监听，回掉执行在异步下载线程，不会阻塞ui线程。
         *
         * @param onDownloadSuccessListener 下载成功监听
         * @return DownloadGenerator
         */
        public DownloadGenerator setOnDownloadSuccessListener(OnDownloadSuccessListener onDownloadSuccessListener) {
            downloadRequest.onDownloadSuccessListener = onDownloadSuccessListener;
            return this;
        }

        /**
         * Tag download task, can use {@link Pump#getDownloadListByTag(String)} to get download list filter by tag,and use {@link DownloadInfo#getTag()} to get tag.
         *
         * @param tag tag
         * @return
         */
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

    public interface OnDownloadSuccessListener {
        void onDownloadSuccess(File downloadFile, DownloadRequest downloadRequest);
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
