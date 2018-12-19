package com.huxq17.download;

import android.os.SystemClock;

import com.huxq17.download.action.Action;
import com.huxq17.download.action.CheckCacheAction;
import com.huxq17.download.action.CorrectDownloadInfoAction;
import com.huxq17.download.action.MergeFileAction;
import com.huxq17.download.action.StartDownloadAction;
import com.huxq17.download.action.VerifyResultAction;
import com.huxq17.download.task.DownloadTask;

import java.util.ArrayList;
import java.util.List;

public class DownloadChain {
    private DownloadTask downloadTask;
    private List<Action> actions;
    private int index;
    private int tryCount;
    private int retryCount;
    private int retryInterval;
    private boolean retry;

    public DownloadChain(DownloadTask downloadTask) {
        List<Action> actions = new ArrayList<>();
        actions.add(new CheckCacheAction());
        actions.add(new CorrectDownloadInfoAction());
        actions.add(new StartDownloadAction());
        actions.add(new MergeFileAction());
        this.downloadTask = downloadTask;
        this.actions = actions;
        index = 0;
        tryCount = 0;
        DownloadRequest request = downloadTask.getRequest();
        retryCount = request.getRetryCount();
        retryInterval = request.getRetryInterval();
    }

    public void downgrade() {
        retryCount++;
        retry = true;
        downloadTask.downgrade();
    }

    public void retry() {
        retry = true;
    }

    public boolean needRetry() {
        return retry && retryCount > tryCount;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void proceed() {
        int actionSize = actions.size();
        while (index != actionSize) {
            Action action = actions.get(index);
            boolean result = action.proceed(this);
            boolean shouldStop = downloadTask.shouldStop();
            if (shouldStop) {
                break;
            } else if (needRetry()) {
                tryCount++;
                retry = false;
                index = 0;
                if (retryInterval > 0) {
                    SystemClock.sleep(retryInterval);
                }
            } else if (result) {
                index++;
            } else {
                break;
            }
        }
        new VerifyResultAction().proceed(this);
    }
}