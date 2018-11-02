package com.huxq17.download;

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

    public DownloadChain(DownloadTask downloadTask) {
        List<Action> actions = new ArrayList<>();
        actions.add(new GetContentLengthAction());
        actions.add(new CorrectDownloadInfoAction());
        actions.add(new StartDownloadAction());
        actions.add(new MergeFileAction());
        this.downloadTask = downloadTask;
        this.actions = actions;
    }

    public void proceed() {
        for (Action action : actions) {
            boolean result = action.proceed(downloadTask);
            boolean shouldStop = downloadTask.shouldStop();
            if (!result || shouldStop) {
                //notify client if action failed.
//                if (!result) {
//                    downloadTask.notifyProgressChanged(downloadTask.getDownloadInfo());
//                }
                break;
            }
        }
        new VerifyResultAction().proceed(downloadTask);
    }
}
