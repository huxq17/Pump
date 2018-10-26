package com.huxq17.download.message;

import android.content.Context;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.listener.DownloadObserver;

public interface IMessageCenter {
    void start(Context context);

    void register(DownloadObserver downloadObserver);

    void unRegister(DownloadObserver downloadObserver);

    void notifyProgressChanged(DownloadInfo downloadInfo);

}
