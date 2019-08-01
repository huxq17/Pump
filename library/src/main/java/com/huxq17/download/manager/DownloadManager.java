package com.huxq17.download.manager;


import android.content.Context;

import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.DownloadService;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.db.DBService;
import com.huxq17.download.listener.DownLoadLifeCycleObserver;
import com.huxq17.download.task.DownloadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class DownloadManager implements IDownloadManager, DownLoadLifeCycleObserver {
    private Context context;
    private ConcurrentHashMap<String, DownloadTask> taskMap;
    private ConcurrentHashMap<String, DownloadDetailsInfo> downloadInfoMap;

    /**
     * 允许同时下载的任务数量
     */
    private int maxRunningTaskNumber = 3;
    private DownloadService downloadService;

    private DownloadManager() {
        taskMap = new ConcurrentHashMap<>();
        downloadService = new DownloadService(this);
    }

    @Override
    public void start(Context context) {
        this.context = context;
    }

    private void startDownloadService() {
        if (!downloadService.isRunning()) {
            downloadService.start();
        }
    }

    private DownloadDetailsInfo createDownloadInfo(String id, String url, String filePath, String tag) {
        DownloadDetailsInfo downloadInfo = null;
        if (downloadInfoMap != null) {
            downloadInfo = downloadInfoMap.get(id);
        }
        if (downloadInfo == null) {
            downloadInfo = DBService.getInstance().getDownloadInfo(id);
        }
        if (downloadInfo != null) {
            return downloadInfo;
        }
        //create a new instance if not found.
        downloadInfo = new DownloadDetailsInfo(url, filePath, tag, id);
        downloadInfo.setCreateTime(System.currentTimeMillis());
        DBService.getInstance().updateInfo(downloadInfo);
        return downloadInfo;
    }

    public void submit(DownloadRequest downloadRequest) {
        startDownloadService();
        String id = downloadRequest.getId();
        String filePath = downloadRequest.getFilePath();
        File downloadFile = new File(filePath);
        if (taskMap.get(id) != null) {
            //The task is running,we need do nothing.
            LogUtil.e("task " + downloadFile.getName() + " is running,we need do nothing.");
            return;
        }
        DownloadDetailsInfo downloadInfo = null;
        if (downloadInfoMap != null) {
            downloadInfo = downloadInfoMap.get(id);
        }
        if (downloadInfo != null) {
            downloadRequest.setDownloadInfo(downloadInfo);
        }
        downloadService.addDownloadRequest(downloadRequest);
    }

    public void delete(DownloadInfo downloadInfo) {
        if (downloadInfo == null) return;
        if (downloadInfoMap != null) {
            downloadInfoMap.remove(downloadInfo.getId());
        }
        synchronized (downloadInfo) {
            DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
            DownloadTask downloadTask = transferInfo.getDownloadTask();
            if (downloadTask == null) {
                downloadTask = taskMap.get(downloadInfo.getId());
            }
            if (downloadTask != null) {
                downloadTask.delete();
            }
            transferInfo.getDownloadFile().delete();
            Util.deleteDir(transferInfo.getTempDir());
            DBService.getInstance().deleteInfo(downloadInfo.getId());
        }
    }

    public void delete(String tag) {
        if (tag == null) return;
        List<DownloadDetailsInfo> tasks = DBService.getInstance().getDownloadListByTag(tag);
        for (DownloadDetailsInfo info : tasks) {
            delete(info);
        }
    }

    @Override
    public void stop(DownloadInfo downloadInfo) {
        DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null) {
            downloadTask.stop();
        }
    }

    @Override
    public void pause(DownloadInfo downloadInfo) {
        for (DownloadTask task : taskMap.values()) {
            if (task.getDownloadInfo() == downloadInfo) {
                task.pause();
            }
        }
    }

    @Override
    public synchronized void resume(DownloadInfo downloadInfo) {
        DownloadDetailsInfo transferInfo = (DownloadDetailsInfo) downloadInfo;
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null && downloadTask.getRequest() != null) {
            DownloadRequest downloadRequest = downloadTask.getRequest();
            submit(downloadRequest);
        } else {
            DownloadRequest.newRequest(transferInfo.getUrl(), transferInfo.getFilePath()).submit();
        }
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadingList() {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        List<DownloadDetailsInfo> list = getAllDownloadList();
        for (DownloadDetailsInfo info : list) {
            if (!info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadedList() {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        List<DownloadDetailsInfo> list = getAllDownloadList();
        for (DownloadDetailsInfo info : list) {
            if (info.isFinished()) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getDownloadListByTag(String tag) {
        List<DownloadDetailsInfo> downloadList = new ArrayList<>();
        List<DownloadDetailsInfo> list = getAllDownloadList();
        for (DownloadDetailsInfo info : list) {
            if (info.getTag().equals(tag)) {
                downloadList.add(info);
            }
        }
        return downloadList;
    }

    @Override
    public List<DownloadDetailsInfo> getAllDownloadList() {
        if (downloadInfoMap == null) {
            downloadInfoMap = new ConcurrentHashMap<>();
            List<DownloadDetailsInfo> list = DBService.getInstance().getDownloadList();
            for (DownloadDetailsInfo transferInfo : list) {
                String id = transferInfo.getId();
                DownloadTask downloadTask = taskMap.get(id);
                if (downloadTask != null) {
                    downloadInfoMap.put(id, downloadTask.getDownloadInfo());
                } else {
                    downloadInfoMap.put(id, transferInfo);
                }
            }
        }
        return new ArrayList<>(downloadInfoMap.values());
    }

    @Override
    public boolean hasDownloadSucceed(String url) {
        DownloadDetailsInfo info = DBService.getInstance().getDownloadInfo(url);
        if (info != null && info.isFinished()) {
            return true;
        }
        return false;
    }

    @Override
    public File getFileIfSucceed(String url) {
        if (hasDownloadSucceed(url)) {
            DownloadDetailsInfo info = DBService.getInstance().getDownloadInfo(url);
            return info.getDownloadFile();
        }
        return null;
    }

    @Override
    public void setDownloadConfig(DownloadConfig downloadConfig) {
        maxRunningTaskNumber = downloadConfig.getMaxRunningTaskNumber();
        downloadService.setMaxRunningTaskNumber(maxRunningTaskNumber);
    }

    @Override
    public synchronized void shutdown() {
        downloadService.cancel();
        for (DownloadTask downloadTask : taskMap.values()) {
            if (downloadTask != null)
                downloadTask.stop();
        }
        DownloadInfoSnapshot.release();
        DBService.getInstance().close();
    }

    public boolean isShutdown() {
        return !downloadService.isRunning();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void onDownloadStart(DownloadTask downloadTask) {
        DownloadInfo downloadInfo = downloadTask.getDownloadInfo();
        if (downloadInfoMap != null && downloadInfo != null) {
            downloadInfoMap.put(downloadTask.getId(), downloadTask.getDownloadInfo());
        }
        taskMap.put(downloadTask.getId(), downloadTask);
    }

    @Override
    public void onDownloadEnd(DownloadTask downloadTask) {
        LogUtil.d("Task " + downloadTask.getName() + " is stopped.");
        taskMap.remove(downloadTask.getId());
    }
}
