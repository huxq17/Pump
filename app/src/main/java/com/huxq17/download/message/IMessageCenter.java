package com.huxq17.download.message;

import android.content.Context;

import com.huxq17.download.DownloadInfo;

public interface IMessageCenter {
    void start(Context context);

    void notifyProgressChanged(DownloadInfo downloadInfo);

}
