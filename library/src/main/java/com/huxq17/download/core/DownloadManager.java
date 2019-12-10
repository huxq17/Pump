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
import java.util.concurrent.ConcurrentHashMap;

public class DownloadManager implements IDownloadManager, DownLoadLifeCycleObserver {
    private Context context;
    private ConcurrentHashMap<String, DownloadTask> taskMap;
    private ConcurrentHashMap<String, DownloadDetailsInfo> downloadInfoMap;

    private DownloadDispatcherThread downloadDispatcherThread;

    private boolean hasFetchDownloadList;

    private DownloadManager() {
        taskMap = new ConcurrentHashMap<>();
        downloadInfoMap = new ConcurrentHashMap<>();
        downloadDispatcherThread = new DownloadDispatcherThread(this);
    }

    @Override
    public void start(Context context) {
        this.context = context;
    }

    public void submit(DownloadRequest downloadRequest) {
        String id = downloadRequest.getId();
        if (taskMap.get(id) != null) {
            //The task is running,we need do nothing.
            LogUtil.e("task " + downloadRequest.getName() + " is running,we need do nothing.");
            return;
        }
        DownloadDetailsInfo downloadInfo = downloadInfoMap.get(id);
        if (downloadInfo != null) {
            downloadRequest.setDownloadInfo(downloadInfo);
        }
        downloadDispatcherThread.enqueueRequest(downloadRequest);
    }

    public void deleteById(String id) {
        if (TextUtils.isEmpty(id)) {
            throw new IllegalArgumentException("Id is empty.");
        }
        DownloadTask downloadTask = taskMap.get(id);
        if (downloadTask != null) {
            synchronized (downloadTask.getLock()) {
                downloadTask.delete();
                DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
                deleteDownloadInfo(downloadInfo);
            }
        } else {
            DownloadDetailsInfo downloadInfo = downloadInfoMap.get(id);
            if (downloadInfo == null) {
                downloadInfo = DBService.getInstance().getDownloadInfo(id);
            }
            deleteDownloadInfo(downloadInfo);
        }
    }

    private void deleteDownloadInfo(DownloadDetailsInfo downloadInfo) {
        if (downloadInfo != null) {
            downloadInfoMap.remove(downloadInfo.getId());
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
        if (TextUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("Tag is empty.");
        }
        List<DownloadInfo> tasks = getDownloadListByTag(tag);
        for (DownloadInfo info : tasks) {
            delete(info);
        }
    }

    public void delete(DownloadInfo downloadInfo) {
        checkDownloadInfo(downloadInfo);
        deleteById(downloadInfo.getId());
    }

    @Override
    public void stop(DownloadInfo downloadInfo) {
        checkDownloadInfo(downloadInfo);
        DownloadTask downloadTask = taskMap.get(downloadInfo.getId());
        if (downloadTask != null) {
            downloadTask.stop();
        }
    }

    @Override
    public void pause(DownloadInfo downloadInfo) {
        checkDownloadInfo(downloadInfo);
        DownloadTask downloadTask = taskMap.get(downloadInfo.getId());
        if (downloadTask != null) {
            downloadTask.pause();
        }
    }

    @Override
    public void resume(DownloadInfo downloadInfo) {
        checkDownloadInfo(downloadInfo);
        DownloadDetailsInfo transferInfo = downloadInfo.getDownloadDetailsInfo();
        DownloadTask downloadTask = transferInfo.getDownloadTask();
        if (downloadTask != null && downloadTask.getRequest() != null) {
            DownloadRequest downloadRequest = downloadTask.getRequest();
            submit(downloadRequest);
        } else {
            DownloadRequest.newRequest(transferInfo.getUrl(), transferInfo.getFilePath()).submit();
        }
    }

    private void checkDownloadInfo(DownloadInfo downloadInfo) {
        if (downloadInfo == null) {
            throw new IllegalArgumentException("downloadInfo is null.");
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
                String id = downloadDetailsInfo.getId();
                if (downloadInfoMap.get(id) == null) {
                    downloadInfoMap.put(id, downloadDetailsInfo);
                } else {
                    downloadDetailsInfo = downloadInfoMap.get(id);
                }
                if (filter == null || filter.filter(downloadDetailsInfo)) {
                    downloadList.add(downloadDetailsInfo.snapshot());
                }
            }
        } else {
            for (DownloadDetailsInfo downloadDetailsInfo : downloadInfoMap.values()) {
                if (filter == null || filter.filter(downloadDetailsInfo)) {
                    downloadList.add(downloadDetailsInfo.snapshot());
                }
            }
        }
        return downloadList;
    }

    public DownloadInfo getDownloadInfoById(String id) {
        DownloadDetailsInfo downloadDetailsInfo;
        downloadDetailsInfo = downloadInfoMap.get(id);
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
        return taskMap.get(id) != null;
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
        downloadDispatcherThread.cancel();
        for (DownloadTask downloadTask : taskMap.values()) {
            if (downloadTask != null)
                downloadTask.stop();
        }
        DownloadInfoSnapshot.release();
        DBService.getInstance().close();
    }

    public boolean isShutdown() {
        return !downloadDispatcherThread.isRunning();
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public void onDownloadStart(DownloadTask downloadTask) {
        downloadInfoMap.put(downloadTask.getId(), downloadTask.getDownloadInfo());
        taskMap.put(downloadTask.getId(), downloadTask);

    }

    @Override
    public void onDownloadEnd(DownloadTask downloadTask) {
        LogUtil.d("Task " + downloadTask.getName() + " is stopped.");
        taskMap.remove(downloadTask.getId());
    }
}
