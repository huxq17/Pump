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

import static com.huxq17.download.ErrorCode.NETWORK_UNAVAILABLE;


public class DownloadChain {
    private DownloadTask downloadTask;
    private List<Action> actions;
    private int index;
    private int tryCount;
    private int retryCount;
    private int retryDelay;

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
        retryDelay = request.getRetryDelay();
    }

    public void downgrade() {
        retryCount++;
        downloadTask.downgrade();
    }

    public boolean isFinishedFromCache() {
        return index == 0;
    }

    public boolean isRetryable() {
        return (downloadTask.isDowngrade() || downloadTask.getDownloadInfo().getErrorCode() == NETWORK_UNAVAILABLE) && retryCount > tryCount;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void proceed() {
        int actionSize = actions.size();
        while (index != actionSize) {
            Action action = actions.get(index);
            boolean result ;
            if(!downloadTask.shouldStop()){
                result = action.proceed(this);
            }else {
                break;
            }

          if (result) {
                index++;
            } else {
                if (isRetryable()) {
                    tryCount++;
                    index = 0;
                    downloadTask.setErrorCode(0);
                    if (retryDelay > 0) {
                        SystemClock.sleep(retryDelay);
                    }
                } else {
                    break;
                }
            }
        }
        new VerifyResultAction().proceed(this);
    }
}