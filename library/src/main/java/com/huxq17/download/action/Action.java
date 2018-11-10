package com.huxq17.download.action;

import com.huxq17.download.DownloadChain;

public interface Action {
    boolean proceed(DownloadChain chain);
}
