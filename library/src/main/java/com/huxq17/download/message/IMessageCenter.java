package com.huxq17.download.message;

import android.content.Context;

import com.huxq17.download.core.DownloadDetailsInfo;

public interface IMessageCenter {
    void start(Context context);

    void register(DownloadListener downloadListener);

    void unRegister(String url);

    @Deprecated
    void unRegister(DownloadListener downloadListener);

    void notifyProgressChanged(DownloadDetailsInfo downloadInfo);

}
