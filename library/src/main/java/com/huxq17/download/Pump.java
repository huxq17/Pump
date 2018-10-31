package com.huxq17.download;


import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.listener.DownloadObserver;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.IMessageCenter;

import java.util.List;

public class Pump {
    public static void download(String url, String filePath) {
        ServiceAgency.getService(IDownloadManager.class).submit(url, filePath);
    }

    public static void pause(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).pause(downloadInfo);
    }
    public static void stop(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).stop(downloadInfo);
    }

    public static void delete(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).delete(downloadInfo);
    }

    public static void reStart(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).reStart(downloadInfo);
    }

    public static void subscribe(DownloadObserver observer) {
        ServiceAgency.getService(IMessageCenter.class).register(observer);
    }

    public static void unSubscribe(DownloadObserver observer) {
        ServiceAgency.getService(IMessageCenter.class).unRegister(observer);
    }

    public static void setDownloadConfig(DownloadConfig downloadConfig) {
        ServiceAgency.getService(IDownloadManager.class).setDownloadConfig(downloadConfig);
    }

    public static List<? extends DownloadInfo> getDownloadingList() {
        return ServiceAgency.getService(IDownloadManager.class).getDownloadingList();
    }

    public static List<? extends DownloadInfo> getDownloadedList() {
        return ServiceAgency.getService(IDownloadManager.class).getDownloadedList();
    }

    public static List<? extends DownloadInfo> getAllDownloadList() {
        return ServiceAgency.getService(IDownloadManager.class).getAllDownloadList();
    }
}
