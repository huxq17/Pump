package com.huxq17.download;

import com.huxq17.download.core.DownloadInfo;

public class DownloadInfoSnapshot {
    public long completedSize;
    public DownloadInfo.Status status;
    public DownloadInfo downloadInfo;
    private static DownloadInfoSnapshot sPool;
    private static int sPoolSize = 0;
    private DownloadInfoSnapshot next;


    public static DownloadInfoSnapshot obtain() {
        synchronized (DownloadInfoSnapshot.class) {
            if (sPool != null) {
                DownloadInfoSnapshot snapshot = sPool;
                sPool = snapshot.next;
                snapshot.next = null;
                sPoolSize--;
                return snapshot;
            }
        }
        return new DownloadInfoSnapshot();
    }

    public void recycle() {
        completedSize = 0;
        status = null;
        downloadInfo = null;
        synchronized (DownloadInfoSnapshot.class) {
            next = sPool;
            sPool = this;
            sPoolSize++;
        }
    }

    public static void release() {
        synchronized (DownloadInfoSnapshot.class) {
            while (sPool != null) {
                DownloadInfoSnapshot snapshot = sPool;
                sPool = snapshot.next;
                snapshot.next = null;
                sPoolSize--;
            }
        }
    }

    public static int getPoolSize() {
        return sPoolSize;
    }
}