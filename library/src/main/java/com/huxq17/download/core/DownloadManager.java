package com.huxq17.download.core;


import android.content.Context;
import android.text.TextUtils;

import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.callback.Filter;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.db.DBService;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadManager implements IDownloadManager {
    private Context context;
    private DownloadInfoManager downloadInfoManager;

    private DownloadDispatcher downloadDispatcher;

    private volatile boolean hasFetchDownloadList;

    private DownloadManager() {
        downloadInfoManager = DownloadInfoManager.getInstance();
        downloadDispatcher = new DownloadDispatcher(this);
    }

    @Override
    public void start(Context context) {
        this.context = context;
    }

    public void submit(DownloadRequest downloadRequest) {
        String id = downloadRequest.getId();
        if (isTaskRunning(id)) {
            //The task is running,we need do nothing.
            LogUtil.e("task " + downloadRequest.getName() + " is running,we need do nothing.");
            return;
        }
        DownloadDetailsInfo downloadInfo = downloadInfoManager.get(id);
        if (downloadInfo != null) {
            downloadRequest.setDownloadInfo(downloadInfo);
        }
        downloadDispatcher.enqueueRequest(downloadRequest);
    }

    public void deleteById(String id) {
        if (TextUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Id is empty.");
        }
        DownloadTask downloadTask = getDownloadTaskById(id);
        if (downloadTask != null) {
            synchronized (downloadTask.getLock()) {
                downloadTask.delete();
                DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
                deleteDownloadInfo(downloadInfo);
            }
        } else {
            DownloadDetailsInfo downloadInfo = downloadInfoManager.get(id);
            if (downloadInfo == null) {
                downloadInfo = DBService.getInstance().getDownloadInfo(id);
            }
            deleteDownloadInfo(downloadInfo);
        }
    }

    private void deleteDownloadInfo(DownloadDetailsInfo downloadInfo) {
        if (downloadInfo != null) {
            downloadInfoManager.remove(downloadInfo.getId());
            if (downloadInfo.getDownloadFile() != null) {
                downloadInfo.getDownloadFile().delete();
            }
            if (downloadInfo.getTempDir() != null) {
                FileUtil.deleteDir(downloadInfo.getTempDir());
            }
            DBService.getInstance().deleteInfo(downloadInfo.getId());
        }
    }

    public void deleteByTag(String tag) {
        List<DownloadInfo> tasks = getDownloadListByTag(tag);
        for (DownloadInfo info : tasks) {
            deleteById(info.getId());
        }
    }

    @Override
    public void stop(String id) {
        checkId(id);
        DownloadTask downloadTask = getDownloadTaskById(id);
        if (downloadTask != null) {
            downloadTask.stop();
        }
    }

    @Override
    public void pause(String id) {
        checkId(id);
        DownloadTask downloadTask = getDownloadTaskById(id);
        if (downloadTask != null) {
            downloadTask.pause();
        }
    }

    private void checkId(String id) {
        if (id == null || id.length() == 0) {
            throw new IllegalArgumentException("id is empty.");
        }
    }

    @Override
    public void resume(String id) {
        checkId(id);
        DownloadDetailsInfo transferInfo = DownloadInfoManager.getInstance().get(id);
        if (transferInfo == null) return;
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null && downloadTask.getRequest() != null) {
            DownloadRequest downloadRequest = downloadTask.getRequest();
            submit(downloadRequest);
        } else {
            DownloadRequest.newRequest(transferInfo.getUrl(), transferInfo.getFilePath()).submit();
        }
    }

    @Override
    public List<DownloadInfo> getDownloadingList() {
        return getDownloadList(new Filter<DownloadDetailsInfo>() {
            @Override
            public boolean filter(DownloadDetailsInfo downloadDetailsInfo) {
                return !downloadDetailsInfo.isFinished();
            }
        });
    }

    @Override
    public List<DownloadInfo> getDownloadedList() {
        return getDownloadList(new Filter<DownloadDetailsInfo>() {
            @Override
            public boolean filter(DownloadDetailsInfo downloadDetailsInfo) {
                return downloadDetailsInfo.isFinished();
            }
        });
    }

    @Override
    public List<DownloadInfo> getDownloadListByTag(final String tag) {
        return getDownloadList(new Filter<DownloadDetailsInfo>() {
            @Override
            public boolean filter(DownloadDetailsInfo downloadDetailsInfo) {
                return downloadDetailsInfo.getTag().equals(tag);
            }
        });
    }

    @Override
    public List<DownloadInfo> getAllDownloadList() {
        return getDownloadList(null);
    }

    private List<DownloadInfo> getDownloadList(Filter<DownloadDetailsInfo> filter) {
        List<DownloadInfo> downloadList = new ArrayList<>();
        if (!hasFetchDownloadList) {
            hasFetchDownloadList = true;
            List<DownloadDetailsInfo> list = DBService.getInstance().getDownloadList();
            for (DownloadDetailsInfo downloadDetailsInfo : list) {
                if (filter == null || filter.filter(downloadDetailsInfo)) {
                    downloadList.add(downloadDetailsInfo.snapshot());
                }
            }
        } else {
            for (DownloadDetailsInfo downloadDetailsInfo : downloadInfoManager.getAll()) {
                if (filter == null || filter.filter(downloadDetailsInfo)) {
                    downloadList.add(downloadDetailsInfo.snapshot());
                }
            }
        }
        return downloadList;
    }

    public DownloadInfo getDownloadInfoById(String id) {
        DownloadDetailsInfo downloadDetailsInfo = downloadInfoManager.get(id);
        if (downloadDetailsInfo == null) {
            downloadDetailsInfo = DBService.getInstance().getDownloadInfo(id);
        }
        return downloadDetailsInfo != null ? downloadDetailsInfo.snapshot() : null;
    }

    @Override
    public boolean hasDownloadSucceed(String id) {
        DownloadDetailsInfo info = DBService.getInstance().getDownloadInfo(id);
        return info != null && info.isFinished();
    }

    public boolean isTaskRunning(String id) {
        DownloadTask downloadTask = getDownloadTaskById(id);
        return downloadTask != null && downloadTask.isRunning();
    }

    public DownloadTask getDownloadTaskById(String id) {
        DownloadDetailsInfo downloadDetailsInfo = downloadInfoManager.get(id);
        if (downloadDetailsInfo != null) {
            return downloadDetailsInfo.getDownloadTask();
        }
        return null;
    }

    @Override
    public File getFileIfSucceed(String id) {
        if (hasDownloadSucceed(id)) {
            DownloadDetailsInfo info = DBService.getInstance().getDownloadInfo(id);
            return info.getDownloadFile();
        }
        return null;
    }

    @Override
    public void shutdown() {
        downloadDispatcher.cancel();
        downloadInfoManager.clear();
        DownloadInfoSnapshot.release();
        DBService.getInstance().close();
        hasFetchDownloadList = false;
    }

    public boolean isShutdown() {
        return !downloadDispatcher.isRunning();
    }

    @Override
    public Context getContext() {
        return context;
    }
}
