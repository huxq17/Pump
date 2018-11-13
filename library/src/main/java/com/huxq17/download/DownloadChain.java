package com.huxq17.download;

import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.action.Action;
import com.huxq17.download.action.CorrectDownloadInfoAction;
import com.huxq17.download.action.GetContentLengthAction;
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
    private boolean isRetry;

    public DownloadChain(DownloadTask downloadTask) {
        List<Action> actions = new ArrayList<>();
        actions.add(new GetContentLengthAction());
        actions.add(new CorrectDownloadInfoAction());
        actions.add(new StartDownloadAction());
        actions.add(new MergeFileAction());
        this.downloadTask = downloadTask;
        this.actions = actions;
        index = 0;
    }

    public void retry() {
        isRetry = true;
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
            } else if (isRetry) {
                index = 0;
                LogUtil.d("retry");
                isRetry = false;
            } else if (result) {
                index++;
            } else {
                break;
            }
        }
        new VerifyResultAction().proceed(this);
    }
}