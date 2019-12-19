package com.huxq17.download.core.interceptor;

import android.text.TextUtils;

import com.huxq17.download.OnVerifyMd5Listener;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.db.DBService;
import com.huxq17.download.provider.Provider;
import com.huxq17.download.utils.FileUtil;

import java.io.File;

public class VerifyResultInterceptor implements DownloadInterceptor {
    @Override
    public DownloadInfo intercept(DownloadChain chain) {
        DownloadRequest downloadRequest = chain.request();
        DownloadDetailsInfo downloadInfo = downloadRequest.getDownloadInfo();
        DownloadTask downloadTask = downloadInfo.getDownloadTask();
        synchronized (downloadTask.getLock()) {
            DownloadInfo.Status status = downloadInfo.getStatus();
            if (downloadTask.isDeleted()) {
                FileUtil.deleteDir(downloadInfo.getTempDir());
                FileUtil.deleteFile(downloadInfo.getDownloadFile());
                downloadInfo.setStatus(DownloadInfo.Status.STOPPED);
                DBService.getInstance().deleteInfo(downloadInfo.getId());
            } else {
                long completedSize = downloadInfo.getCompletedSize();
                long contentLength = downloadInfo.getContentLength();
                File downloadFile = downloadInfo.getDownloadFile();
                downloadInfo.setFinished(0);
                long downloadFileLength = downloadFile == null ? 0 : downloadFile.length();
                if (completedSize > 0 && completedSize == contentLength && downloadFileLength == contentLength &&
                        isMd5Equals(downloadInfo.getMd5(), downloadInfo.getDownloadFile(), downloadRequest.getOnVerifyMd5Listener())) {
                    Provider.CacheBean cacheBean = downloadInfo.getCacheBean();
                    if (cacheBean != null) {
                        DBService.getInstance().updateCache(cacheBean);
                    }
                    if (downloadRequest.getOnDownloadSuccessListener() != null) {
                        downloadRequest.getOnDownloadSuccessListener().onDownloadSuccess(downloadInfo.getDownloadFile(), downloadRequest);
                    }
                    downloadInfo.setFinished(1);
                    downloadInfo.setStatus(DownloadInfo.Status.FINISHED);
                } else if (status.isRunning()) {
                    downloadInfo.setStatus(DownloadInfo.Status.FAILED);
                } else if (status == DownloadInfo.Status.PAUSING) {
                    downloadInfo.setStatus(DownloadInfo.Status.PAUSED);
                }
                downloadTask.notifyProgressChanged(downloadInfo);
                downloadTask.updateInfo();
            }
        }
        return downloadInfo.snapshot();
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
