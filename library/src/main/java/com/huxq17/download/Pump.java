package com.huxq17.download;


import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.listener.DownloadObserver;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.IMessageCenter;

public class Pump {
    public static Request from(String url) {
        return new Request(url);
    }

    public void subscribe(DownloadObserver observer) {
        ServiceAgency.getService(IMessageCenter.class).register(observer);
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

        public void subscribe(DownloadObserver observer) {
            observer.setDownloadInfo(downloadInfo);
            ServiceAgency.getService(IMessageCenter.class).register(observer);
            ServiceAgency.getService(IDownloadManager.class).submit(downloadInfo);
        }
    }

}
