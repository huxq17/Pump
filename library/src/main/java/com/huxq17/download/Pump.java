package com.huxq17.download;


import android.net.Uri;

import androidx.annotation.NonNull;

import com.huxq17.download.config.DownloadConfig;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadListener;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.core.service.IMessageCenter;

import java.io.File;
import java.util.List;

public class Pump {
    public static DownloadConfig.Builder newConfigBuilder() {
        return DownloadConfig.newBuilder();
    }

    /**
     * Create a new download request,download file will store in cache path.
     *
     * @param url remote url
     */
    public static DownloadRequest.DownloadGenerator newRequest(String url) {
        return newRequest(url, null, null);
    }

    /**
     * Create a new download request.
     *
     * @param url       remote url
     * @param directory the directory that save download file or full path.
     */
    public static DownloadRequest.DownloadGenerator newRequest(String url, String directory) {
        return newRequest(url, directory, null);
    }

    /**
     * Create a new download request.
     *
     * @param url       remote url
     * @param directory the directory that save download file
     * @param fileName  specify file name
     */
    public static DownloadRequest.DownloadGenerator newRequest(String url, String directory, String fileName) {
        return newRequest(url, directory, null, null);
    }

    /**
     * Create a new download request.
     *
     * @param url       remote url
     * @param directory the directory that save download file
     * @param fileName  specify file name
     */
    public static DownloadRequest.DownloadGenerator newRequest(String url, String directory, String fileName, Uri uri) {
        String filePath = directory != null ? (directory + File.separator + (fileName == null ? "" : fileName)) : null;
        return DownloadRequest.newRequest(url, filePath, uri);
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
     * @param downloadListener downloadListener
     */
    public static void unSubscribe(DownloadListener downloadListener) {
        PumpFactory.getService(IMessageCenter.class).unRegister(downloadListener);
    }

    /**
     * Pause a download task by {@link DownloadInfo}
     *
     * @param id unique download id,default is download url.
     */
    public static void pause(String id) {
        PumpFactory.getService(IDownloadManager.class).pause(id);
    }

    /**
     * Stop a download task by id
     *
     * @param id unique download id,default is download url.
     */
    public static void stop(String id) {
        PumpFactory.getService(IDownloadManager.class).stop(id);
    }

    /**
     * Delete a download by Tag,to delete a group of task.
     *
     * @param tag tag th tag of group.
     */
    public static void deleteByTag(String tag) {
        PumpFactory.getService(IDownloadManager.class).deleteByTag(tag);
    }

    /**
     * Delete a download by special download id.
     *
     * @param id unique download id,default is download url.
     */
    public static void deleteById(String id) {
        PumpFactory.getService(IDownloadManager.class).deleteById(id);
    }

    /**
     * Continue a download by id.
     *
     * @param id unique download id,default is download url.
     */
    public static void resume(String id) {
        PumpFactory.getService(IDownloadManager.class).resume(id);
    }

    public static void shutdown() {
        PumpFactory.getService(IDownloadManager.class).shutdown();
    }

    /**
     * Get a list of all download list.
     *
     * @return download list
     */
    public static List<DownloadInfo> getAllDownloadList() {
        return PumpFactory.getService(IDownloadManager.class).getAllDownloadList();
    }

    public static List<DownloadInfo> getDownloadingList() {
        return PumpFactory.getService(IDownloadManager.class).getDownloadingList();
    }

    public static List<DownloadInfo> getDownloadedList() {
        return PumpFactory.getService(IDownloadManager.class).getDownloadedList();
    }

    /**
     * Get download list filter by tag.
     *
     * @param tag tag
     * @return
     */
    public static List<DownloadInfo> getDownloadListByTag(String tag) {
        return PumpFactory.getService(IDownloadManager.class).getDownloadListByTag(tag);
    }

    /**
     * Get downloadInfo by unique download id.
     *
     * @param id unique download id,default is download url.
     * @return
     */
    public static DownloadInfo getDownloadInfoById(String id) {
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
