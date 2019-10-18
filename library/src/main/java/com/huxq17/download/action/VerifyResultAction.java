package com.huxq17.download.action;


import android.text.TextUtils;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.OnVerifyMd5Listener;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.db.DBService;
import com.huxq17.download.provider.Provider;
import com.huxq17.download.task.DownloadTask;

import java.io.File;

public class VerifyResultAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        DownloadTask downloadTask = chain.getDownloadTask();
        DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
        DownloadRequest downloadRequest = downloadTask.getRequest();
        synchronized (downloadTask.getLock()) {
            DownloadInfo.Status status = downloadInfo.getStatus();
            downloadTask.destroy();
            if (downloadTask.isNeedDelete()) {
                Util.deleteDir(downloadInfo.getTempDir());
                Util.deleteFile(downloadInfo.getDownloadFile());
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                DBService.getInstance().deleteInfo(downloadInfo.getId());
            } else {
                long completedSize = downloadInfo.getCompletedSize();
                long contentLength = downloadInfo.getContentLength();
                long downloadFileLength = downloadInfo.getDownloadFile().length();
                if (completedSize > 0 && completedSize == contentLength && downloadFileLength == contentLength &&
                        isMd5Equals(downloadRequest.getMd5(), downloadInfo.getDownloadFile(), downloadRequest.getOnVerifyMd5Listener())) {
                    Provider.CacheBean cacheBean = downloadRequest.getCacheBean();
                    if (cacheBean != null) {
                        DBService.getInstance().updateCache(cacheBean);
                    }
                    if (downloadRequest.getOnDownloadSuccessListener() != null) {
                        downloadRequest.getOnDownloadSuccessListener().onDownloadSuccess(downloadInfo.getDownloadFile(), downloadRequest);
                    }

                    downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                    downloadTask.notifyProgressChanged(downloadInfo);
                } else if (status == DownloadInfo.Status.PAUSING) {
                    downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
                    downloadTask.notifyProgressChanged(downloadInfo);
                } else {
                    downloadInfo.setStatus(DownloadInfo.Status.FAILED);
                    downloadTask.notifyProgressChanged(downloadInfo);
                }
            }
        }
        return true;
    }

    private boolean isMd5Equals(String md5, File downloadFile, OnVerifyMd5Listener listener) {
        if (listener == null) {
            listener = PumpFactory.getService(IDownloadConfigService.class).getOnVerifyMd5Listener();
        }
        if (!TextUtils.isEmpty(md5) && listener != null) {
            return listener.onVerifyMd5(md5, downloadFile);
        }
        return true;
    }
}
