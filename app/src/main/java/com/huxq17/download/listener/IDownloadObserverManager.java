package com.huxq17.download.listener;

import android.content.Context;

public interface IDownloadObserverManager {
    void start(Context context);

    void register(DownloadObserver downloadObserver);

    void unRegister(DownloadObserver downloadObserver);
}
