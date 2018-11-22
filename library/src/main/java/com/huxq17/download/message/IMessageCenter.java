package com.huxq17.download.message;

import android.content.Context;

import com.huxq17.download.DownloadDetailsInfo;

public interface IMessageCenter {
    void start(Context context);

    void register(DownloadObserver downloadObserver);

    @Deprecated
    void unRegister(DownloadObserver downloadObserver);

    void notifyProgressChanged(DownloadDetailsInfo downloadInfo);

}
