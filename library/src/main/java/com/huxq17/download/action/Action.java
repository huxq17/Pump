package com.huxq17.download.action;

import com.huxq17.download.task.DownloadTask;

public interface Action {
    boolean proceed(DownloadTask t);
}
