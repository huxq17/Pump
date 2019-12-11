package com.huxq17.download.core;


import android.text.TextUtils;

import com.huxq17.download.OnVerifyMd5Listener;
import com.huxq17.download.Pump;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.DownloadListener;

import java.io.File;


public final class DownloadRequest {
    private final String id;
    private final String url;
    private final String filePath;
    private final int threadNum;
    private final String tag;
    private final boolean forceReDownload;
    private final int retryCount;
    private final int retryDelay;
    private final OnVerifyMd5Listener onVerifyMd5Listener;
    private final OnDownloadSuccessListener onDownloadSuccessListener;
    //Maybe use in the future
    private final DownloadListener downloadListener;

    private final DownloadTaskExecutor downloadTaskExecutor;
    private DownloadDetailsInfo downloadInfo;

    DownloadRequest(DownloadGenerator downloadGenerator) {
        this.id = downloadGenerator.id;
        this.url = downloadGenerator.url;
        this.filePath = downloadGenerator.filePath;
        this.threadNum = downloadGenerator.threadNum;
        this.tag = downloadGenerator.tag;
        this.forceReDownload = downloadGenerator.forceReDownload;
        this.retryCount = downloadGenerator.retryCount;
        this.retryDelay = downloadGenerator.retryDelay;
        this.onVerifyMd5Listener = downloadGenerator.onVerifyMd5Listener;
        this.onDownloadSuccessListener = downloadGenerator.onDownloadSuccessListener;
        this.downloadListener = downloadGenerator.downloadListener;
        this.downloadTaskExecutor = downloadGenerator.downloadTaskExecutor;
    }

    void setDownloadInfo(DownloadDetailsInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        downloadInfo.setFilePath(filePath);
    }

    public OnVerifyMd5Listener getOnVerifyMd5Listener() {
        return onVerifyMd5Listener;
    }

    public OnDownloadSuccessListener getOnDownloadSuccessListener() {
        return onDownloadSuccessListener;
    }

    public int getRetryDelay() {
        return retryDelay;
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
        return retryCount;
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
        if(downloadTaskExecutor!=null){
            String tag = downloadTaskExecutor.getTag();
            if (tag!= null && tag.length() >0) {
              return tag;
            }
        }
        return tag == null ? "" : tag;
    }

    public boolean isForceReDownload() {
        return forceReDownload;
    }

    public DownloadTaskExecutor getDownloadDispatcher() {
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
        private OnVerifyMd5Listener onVerifyMd5Listener;
        private OnDownloadSuccessListener onDownloadSuccessListener;
        private DownloadListener downloadListener;

        private static final int DEFAULT_RETRY_DELAY = 200;
        private DownloadTaskExecutor downloadTaskExecutor;

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

        public DownloadGenerator setOnVerifyMd5Listener(OnVerifyMd5Listener listener) {
            this.onVerifyMd5Listener = listener;
            return this;
        }

        /**
         * 设置下载成功的监听，回调执行在异步下载线程，不会阻塞ui线程。
         *
         * @param onDownloadSuccessListener 下载成功监听
         * @return DownloadGenerator
         */
        public DownloadGenerator setOnDownloadSuccessListener(OnDownloadSuccessListener onDownloadSuccessListener) {
            this.onDownloadSuccessListener = onDownloadSuccessListener;
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
                this.downloadListener.enable(id);
            }
            PumpFactory.getService(IDownloadManager.class).
                    submit(new DownloadRequest(this));
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
