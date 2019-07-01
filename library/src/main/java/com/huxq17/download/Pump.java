package com.huxq17.download;


import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.DownloadListener;
import com.huxq17.download.message.IMessageCenter;

import java.io.File;
import java.util.List;

public class Pump {
    /**
     * Create a new download request,download file will store in cache path.
     *
     * @param url remote url
     */
    public static DownloadRequest.DownloadGenerator newRequest(String url) {
        return newRequest(url, Util.getCachePathByUrl(PumpFactory.getService(IDownloadManager.class).getContext(), url));
    }

    /**
     * Create a new download request.
     *
     * @param url      remote url
     * @param filePath file download path
     */
    public static DownloadRequest.DownloadGenerator newRequest(String url, String filePath) {
        return DownloadRequest.newRequest(url, filePath);
    }

    /**
     * Use {@link Pump#newRequest(String, String)} instead.
     * Download file from remote url to local file path.
     *
     * @param url      remote url
     * @param filePath file download path
     */
    @Deprecated
    public static void download(String url, String filePath) {
        DownloadRequest.newRequest(url, filePath).submit();
    }

    /**
     * subscribe {@link DownloadListener} to listen download progress.
     *
     * @param downloadListener
     */
    public static void subscribe(DownloadListener downloadListener) {
        PumpFactory.getService(IMessageCenter.class).register(downloadListener);
    }

    /**
     * unSubscribe url download progress.
     *
     * @param url download url
     */
    public static void unSubscribe(String url) {
        PumpFactory.getService(IMessageCenter.class).unRegister(url);
    }

    /**
     * unSubscribe {@link DownloadListener}.
     *
     * @param downloadListener
     */
    @Deprecated
    public static void unSubscribe(DownloadListener downloadListener) {
        PumpFactory.getService(IMessageCenter.class).unRegister(downloadListener);
    }

    /**
     * Pause a download task by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadListener#getDownloadInfo()}
     */
    public static void pause(DownloadInfo downloadInfo) {
        PumpFactory.getService(IDownloadManager.class).pause(downloadInfo);
    }

    /**
     * Stop a download task by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadListener#getDownloadInfo()}
     */
    public static void stop(DownloadInfo downloadInfo) {
        PumpFactory.getService(IDownloadManager.class).stop(downloadInfo);
    }

    /**
     * Delete a download info by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadListener#getDownloadInfo()}
     */
    public static void delete(DownloadInfo downloadInfo) {
        PumpFactory.getService(IDownloadManager.class).delete(downloadInfo);
    }

    /**
     * Delete a download info by Tag
     *
     * @param tag
     */
    public static void delete(String tag) {
        PumpFactory.getService(IDownloadManager.class).delete(tag);
    }

    /**
     * Delete a download info by {@link DownloadInfo}
     *
     * @param url      download url
     * @param filePath
     */
    public static void delete(String url, String filePath) {
        PumpFactory.getService(IDownloadManager.class).delete(new DownloadDetailsInfo(url, filePath));
    }

    /**
     * Continue a download info by {@link DownloadInfo}
     *
     * @param downloadInfo get from {@link DownloadListener#getDownloadInfo()}
     */
    public static void resume(DownloadInfo downloadInfo) {
        PumpFactory.getService(IDownloadManager.class).resume(downloadInfo);
    }

    public static void shutdown() {
        PumpFactory.getService(IDownloadManager.class).shutdown();
    }

    /**
     * Get a list of all download tasks.
     *
     * @return
     */
    public static List<? extends DownloadInfo> getAllDownloadList() {
        return PumpFactory.getService(IDownloadManager.class).getAllDownloadList();
    }

    public static List<? extends DownloadInfo> getDownloadingList() {
        return PumpFactory.getService(IDownloadManager.class).getDownloadingList();
    }

    /**
     * Check url whether download success
     *
     * @param url download url
     * @return true If Pump has downloaded
     */
    public static boolean hasCached(String url) {
        return PumpFactory.getService(IDownloadManager.class).hasCached(url);
    }

    /**
     * If url has downloaded successful,return the local file
     *
     * @param url download url
     * @return the file has downloaded.
     */
    public static File getFileFromCache(String url) {
        return PumpFactory.getService(IDownloadManager.class).getFileFromCache(url);
    }

    public static List<? extends DownloadInfo> getDownloadedList() {
        return PumpFactory.getService(IDownloadManager.class).getDownloadedList();
    }

}
