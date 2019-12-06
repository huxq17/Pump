package com.huxq17.download.core.action;

import com.huxq17.download.DownloadChain;

public interface Action {
    boolean proceed(DownloadChain chain);
}
