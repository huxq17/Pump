package com.huxq17.download;

import android.os.SystemClock;

import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.action.Action;
import com.huxq17.download.core.action.CacheCheckAction;
import com.huxq17.download.core.action.DownloadInfoCorrectAction;
import com.huxq17.download.core.action.FileMergeAction;
import com.huxq17.download.core.action.DownloadExecuteAction;
import com.huxq17.download.core.action.ResultVerifyAction;
import com.huxq17.download.core.task.DownloadTask;

import java.util.ArrayList;
import java.util.List;

import static com.huxq17.download.ErrorCode.NETWORK_UNAVAILABLE;


public class DownloadChain {
    private DownloadTask downloadTask;
    private List<Action> actions;
    private int index;
    private int tryCount;
    private int retryUpperLimit;
    private int retryDelay;

    public DownloadChain(DownloadTask downloadTask) {
        List<Action> actions = new ArrayList<>();
        actions.add(new CacheCheckAction());
        actions.add(new DownloadInfoCorrectAction());
        actions.add(new DownloadExecuteAction());
        actions.add(new FileMergeAction());
        this.downloadTask = downloadTask;
        this.actions = actions;
        index = 0;
        tryCount = 0;
        DownloadRequest request = downloadTask.getRequest();
        retryUpperLimit = request.getRetryCount();
        retryDelay = request.getRetryDelay();
    }

    public boolean isRetryable() {
        return  downloadTask.getDownloadInfo().getErrorCode() == NETWORK_UNAVAILABLE && retryUpperLimit > tryCount;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void proceed() {
        int actionSize = actions.size();
        while (index != actionSize) {
            Action action = actions.get(index);
            boolean result ;
            if(downloadTask.isRunning()){
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
        new ResultVerifyAction().proceed(this);
    }
}