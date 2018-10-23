package com.huxq17.download;


import com.huxq17.download.listener.StatusObserver;

public class Pump {
    public static Request from(String url) {
        return new Request(url);
    }

    public static class Request {
        private DownloadInfo downloadInfo;

        public Request(String url) {
            downloadInfo = new DownloadInfo();
            downloadInfo.url = url;
        }

        public Request into(String filePath) {
            downloadInfo.filePath = filePath;
            return this;
        }

        public void subscribe(StatusObserver observer) {
            downloadInfo.statusObserver = observer;
            DownloadManager.getInstance().submit(downloadInfo);
        }
    }
}
