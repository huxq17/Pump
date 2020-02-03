package com.huxq17.download.core.service;

import android.content.Context;

import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadListener;

public interface IMessageCenter {
    void start(Context context);

    void register(DownloadListener downloadListener);

    void unRegister(String url);

    void unRegister(DownloadListener downloadListener);

    void notifyProgressChanged(DownloadDetailsInfo downloadInfo);

}
