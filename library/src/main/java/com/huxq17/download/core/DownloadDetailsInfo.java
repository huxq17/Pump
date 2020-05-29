package com.huxq17.download.core;

import android.text.TextUtils;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.core.task.DownloadTask;
import com.huxq17.download.db.DBService;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import static com.huxq17.download.utils.Util.CONTENT_LENGTH_NOT_FOUND;
import static com.huxq17.download.utils.Util.DOWNLOAD_PART;

public class DownloadDetailsInfo {
    private WeakReference<Object> wfExtraData;
    protected final String url;
    private String filePath;
    protected final String id;
    private final String tag;
    private final long createTime;

    private long completedSize;
    private long contentLength = CONTENT_LENGTH_NOT_FOUND;
    private int finished;
    protected volatile DownloadInfo.Status status;
    private String speed;
    private int errorCode;
    private File tempDir;
    private List<File> downloadPartFiles = new ArrayList<>();
    private File downloadFile;
    private DownloadTask downloadTask;
    private SpeedMonitor speedMonitor;

    private DownloadProvider.CacheBean cacheBean;
    private String md5;
    private int progress;
    /**
     * True indicate that support breakpoint download.
     */
    private boolean supportBreakpoint = true;

    public DownloadDetailsInfo(String url, String filePath) {
        this(url, filePath, null, url, System.currentTimeMillis());
    }

    public DownloadDetailsInfo(String url, String filePath, String tag, String id, long createTime) {
        this.url = url;
        if (TextUtils.isEmpty(id)) {
            this.id = url;
        } else {
            this.id = id;
        }
        this.tag = tag;
        this.filePath = filePath;
        this.createTime = createTime;
        if (filePath != null) {
            downloadFile = new File(filePath);
        }
        speedMonitor = new SpeedMonitor();

    }

    public boolean isSupportBreakpoint() {
        return supportBreakpoint;
    }

    public void setSupportBreakpoint(boolean supportBreakpoint) {
        this.supportBreakpoint = supportBreakpoint;
    }


    public void setFilePath(String filePath) {
        if (filePath != null && !filePath.equals(this.filePath)) {
            this.filePath = filePath;
            deleteTempDir();
            downloadFile = new File(filePath);
        }
    }

    public void setDownloadTask(DownloadTask downloadTask) {
        this.downloadTask = downloadTask;
    }

    public DownloadTask getDownloadTask() {
        return downloadTask;
    }

    public void updateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return;
        }
        synchronized (this) {
            setFilePath(filePath);
            deleteDownloadFile();
            DBService.getInstance().updateInfo(this);
        }
    }

    public void setCacheBean(DownloadProvider.CacheBean cacheBean) {
        this.cacheBean = cacheBean;
    }

    public DownloadProvider.CacheBean getCacheBean() {
        return cacheBean;
    }

    public String getMd5() {
        return md5 == null ? "" : md5;
    }

    public void setMD5(String md5) {
        this.md5 = md5;
    }

    public void setCompletedSize(long completedSize) {
        this.completedSize = completedSize;
    }

    public void download(int length) {
        this.completedSize += length;
        speedMonitor.download(length);
    }

    public void computeSpeed() {
        this.speed = speedMonitor.getSpeed();
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setFinished(int finished) {
        this.finished = finished;
    }

    public void setStatus(DownloadInfo.Status status) {
        this.status = status;
    }

    public boolean isDeleted() {
        return status == DownloadInfo.Status.DELETED;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setErrorCode(int code) {
        setErrorCode(code, false);
    }

    public void setErrorCode(int code, boolean force) {
        if (status != null && (status.isRunning() || force)) {
            this.errorCode = code;
            setStatus(DownloadInfo.Status.FAILED);
        }
    }

    public void clearErrorCode() {
        this.errorCode = 0;
    }

    public DownloadInfo.Status getStatus() {
        return this.status;
    }

    public File getTempDir() {
        if (tempDir == null && this.filePath != null) {
            tempDir = Util.getTempDir(this.filePath);
        }
        return tempDir;
    }

    public boolean isFinished() {
        synchronized (this) {
            if (downloadFile == null) {
                return false;
            }
            if (this.finished == 1) {
                if (contentLength > 0 && downloadFile.exists() && downloadFile.length() == contentLength) {
                    return true;
                } else if (downloadFile.exists()) {
                    FileUtil.deleteFile(downloadFile);
                }
            }
            this.finished = 0;
            return false;
        }
    }

    /**
     * load completedSize if not finished.
     */
    private void loadDownloadFiles() {
        if (this.filePath == null) return;
        File tempDir = Util.getTempDir(this.filePath);
        tempDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if (name.startsWith(DOWNLOAD_PART)) {
                    File file = new File(dir, name);
                    downloadPartFiles.add(file);
                    completedSize += file.length();
                    return true;
                }
                return false;
            }
        });
    }

    public void calculateDownloadProgress() {
        if (isFinished()) {
            setCompletedSize(this.contentLength);
            if (this.status == null) {
                setStatus(DownloadInfo.Status.FINISHED);
            }
        } else {
            //Only load once.
            if (downloadPartFiles.size() == 0) {
                this.completedSize = 0;
                loadDownloadFiles();
            }
            if (this.status == null) {
                setStatus(DownloadInfo.Status.STOPPED);
            }
        }
        progress = (int) (completedSize * 1f / contentLength * 100);
    }

    public DownloadInfo snapshot() {
        computeSpeed();
        return new DownloadInfo(url, downloadFile, tag, id, createTime, speed, completedSize, contentLength,
                errorCode, status, finished, progress, this);
    }

    public File getDownloadFile() {
        return downloadFile;
    }

    public void deleteDownloadFile() {
        if (downloadFile != null) {
            FileUtil.deleteFile(downloadFile);
        }
    }

    public void deleteTempDir() {
        if (getTempDir() != null) {
            FileUtil.deleteDir(getTempDir());
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public String getName() {
        return downloadFile == null ? "" : downloadFile.getName();
    }

    public String getUrl() {
        return url;
    }

    public String getId() {
        return id;
    }

    public long getContentLength() {
        return contentLength;
    }

    public String getTag() {
        return tag;
    }

    public int getFinished() {
        return finished;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getCompletedSize() {
        return completedSize;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setExtraData(Object extraData) {
        wfExtraData = new WeakReference<>(extraData);
    }

    public Object getWfExtraData() {
        return wfExtraData == null ? null : wfExtraData.get();
    }

    public synchronized boolean isRunning() {
        return status != null && status.isRunning();
    }
}
