package com.huxq17.download;


import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.huxq17.download.listener.DownloadObserver;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.IMessageCenter;

import java.util.List;

public class Pump {
    /**
     * Download file from remote url to local file path.
     *
     * @param url      remote url
     * @param filePath local file path
     */
    public static void download(String url, String filePath) {
        ServiceAgency.getService(IDownloadManager.class).submit(url, filePath);
    }

    /**
     * subscribe {@link DownloadObserver} to observer download progress.
     *
     * @param observer
     */
    public static void subscribe(DownloadObserver observer) {
        ServiceAgency.getService(IMessageCenter.class).register(observer);
    }

    /**
     * unSubscribe {@link DownloadObserver}.
     *
     * @param observer
     */
    public static void unSubscribe(DownloadObserver observer) {
        ServiceAgency.getService(IMessageCenter.class).unRegister(observer);
    }

    /**
     * Pause a download task by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadObserver#getDownloadInfo()}
     */
    public static void pause(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).pause(downloadInfo);
    }

    /**
     * Stop a download task by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadObserver#getDownloadInfo()}
     */
    public static void stop(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).stop(downloadInfo);
    }

    /**
     * Delete a download info by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadObserver#getDownloadInfo()}
     */
    public static void delete(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).delete(downloadInfo);
    }

    /**
     * Continue a download info by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadObserver#getDownloadInfo()}
     */
    public static void reStart(DownloadInfo downloadInfo) {
        ServiceAgency.getService(IDownloadManager.class).reStart(downloadInfo);
    }

    public static void shutdown() {
        ServiceAgency.getService(IDownloadManager.class).shutdown();
    }

    /**
     * Get a list of all download tasks.
     *
     * @return
     */
    public static List<? extends DownloadInfo> getAllDownloadList() {
        return ServiceAgency.getService(IDownloadManager.class).getAllDownloadList();
    }

    public static List<? extends DownloadInfo> getDownloadingList() {
        return ServiceAgency.getService(IDownloadManager.class).getDownloadingList();
    }

    public static List<? extends DownloadInfo> getDownloadedList() {
        return ServiceAgency.getService(IDownloadManager.class).getDownloadedList();
    }

}
