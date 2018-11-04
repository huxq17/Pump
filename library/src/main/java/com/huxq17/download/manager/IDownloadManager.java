package com.huxq17.download.manager;

import android.content.Context;

import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.task.DownloadTask;

import java.util.List;

public interface IDownloadManager {
    void start(Context context);

    void submit(String url, String filePath);

    void delete(DownloadInfo downloadInfo);

    void pause(DownloadInfo downloadInfo);

    void stop(DownloadInfo downloadInfo);

    void resume(DownloadInfo downloadInfo);

    DownloadTask acquireTask() throws InterruptedException;


    List<TransferInfo> getDownloadingList();

    List<TransferInfo> getDownloadedList();

    List<TransferInfo> getAllDownloadList();

    void setDownloadConfig(DownloadConfig downloadConfig);

    void onServiceDestroy();

    void shutdown();
}
