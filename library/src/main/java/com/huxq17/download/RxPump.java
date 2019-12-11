package com.huxq17.download;


import android.support.annotation.NonNull;

import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.message.DownloadListener;
import com.huxq17.download.message.IMessageCenter;

import java.io.File;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

public class RxPump {
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
     * Use {@link RxPump#newRequest(String, String)} instead.
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
    public static Observable<Boolean> delete(final DownloadInfo downloadInfo) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) {
                PumpFactory.getService(IDownloadManager.class).delete(downloadInfo);
                e.onNext(true);
            }
        });
    }

    /**
     * Delete a download info by Tag
     *
     * @param tag tag
     */
    public static Observable<Boolean> deleteByTag(final String tag) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) {
                PumpFactory.getService(IDownloadManager.class).deleteByTag(tag);
                e.onNext(true);
            }
        });
    }

    /**
     * Delete a download info by unique download id. this method may delete a group of tasks.
     *
     * @param id unique download id,default is download url.
     */
    public static Observable<Boolean> deleteById(@NonNull final String id) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) {
                PumpFactory.getService(IDownloadManager.class).deleteById(id);
                e.onNext(true);
            }
        });
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
    public static Observable<List<DownloadInfo>> getAllDownloadList() {
        return Observable.create(new ObservableOnSubscribe<List<DownloadInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<DownloadInfo>> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).getAllDownloadList());
            }
        });
    }

    public static Observable<List<DownloadInfo>> getDownloadingList() {
        return Observable.create(new ObservableOnSubscribe<List<DownloadInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<DownloadInfo>> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).getDownloadingList());
            }
        });
    }

    public static Observable<List<DownloadInfo>> getDownloadedList() {
        return Observable.create(new ObservableOnSubscribe<List<DownloadInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<DownloadInfo>> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).getDownloadedList());
            }
        });
    }

    /**
     * Get download list filter by tag.
     *
     * @param tag tag
     * @return
     */
    public static Observable<List<DownloadInfo>> getDownloadListByTag(final String tag) {
        return Observable.create(new ObservableOnSubscribe<List<DownloadInfo>>() {
            @Override
            public void subscribe(ObservableEmitter<List<DownloadInfo>> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).getDownloadListByTag(tag));
            }
        });
    }

    /**
     * Get downloadInfo by unique download id.
     *
     * @param id unique download id,default is download url.
     * @return
     */
    public static Observable<DownloadInfo> getDownloadInfoById(@NonNull final String id) {
        return Observable.create(new ObservableOnSubscribe<DownloadInfo>() {
            @Override
            public void subscribe(ObservableEmitter<DownloadInfo> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).getDownloadInfoById(id));
            }
        });
    }

    /**
     * Check url whether download success
     *
     * @param id unique download id,default is download url.
     * @return true If Pump has downloaded
     */
    public static Observable<Boolean> hasDownloadSucceed(@NonNull final String id) {
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).hasDownloadSucceed(id));
            }
        });
    }

    /**
     * If url had download successful,return the local file
     *
     * @param id unique download id,default is download url.
     * @return the file has downloaded.
     */
    public static Observable<File> getFileIfSucceed(@NonNull final String id) {
        return Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(ObservableEmitter<File> e) {
                e.onNext(PumpFactory.getService(IDownloadManager.class).getFileIfSucceed(id));
            }
        });
    }

}
