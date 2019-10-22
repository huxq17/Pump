package com.huxq17.download;


import android.support.annotation.NonNull;

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
        return newRequest(url, null);
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
     * @param id unique download id,default is download url.
     */
    public static void unSubscribe(String id) {
        PumpFactory.getService(IMessageCenter.class).unRegister(id);
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
     * @param tag tag
     */
    public static void deleteByTag(String tag) {
        PumpFactory.getService(IDownloadManager.class).deleteByTag(tag);
    }

    /**
     * Delete a download info by unique download id. this method may delete a group of tasks.
     *
     * @param id unique download id,default is download url.
     */
    public static void deleteById(String id) {
        PumpFactory.getService(IDownloadManager.class).deleteById(id);
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
     * Get a list of all download list.
     *
     * @return
     */
    public static List<? extends DownloadInfo> getAllDownloadList() {
        return PumpFactory.getService(IDownloadManager.class).getAllDownloadList();
    }

    public static List<? extends DownloadInfo> getDownloadingList() {
        return PumpFactory.getService(IDownloadManager.class).getDownloadingList();
    }

    public static List<? extends DownloadInfo> getDownloadedList() {
        return PumpFactory.getService(IDownloadManager.class).getDownloadedList();
    }

    /**
     * Get download list filter by tag.
     *
     * @param tag tag
     * @return
     */
    public static List<DownloadDetailsInfo> getDownloadListByTag(String tag) {
        return PumpFactory.getService(IDownloadManager.class).getDownloadListByTag(tag);
    }

    /**
     * Get downloadInfo by unique download id.
     *
     * @param id unique download id,default is download url.
     * @return
     */
    public static DownloadDetailsInfo getDownloadInfoById(String id) {
        return PumpFactory.getService(IDownloadManager.class).getDownloadInfoById(id);
    }

    /**
     * Check url whether download success
     *
     * @param id unique download id,default is download url.
     * @return true If Pump has downloaded
     */
    public static boolean hasDownloadSucceed(@NonNull String id) {
        return PumpFactory.getService(IDownloadManager.class).hasDownloadSucceed(id);
    }

    /**
     * If url had download successful,return the local file
     *
     * @param id unique download id,default is download url.
     * @return the file has downloaded.
     */
    public static File getFileIfSucceed(@NonNull String id) {
        return PumpFactory.getService(IDownloadManager.class).getFileIfSucceed(id);
    }

}
