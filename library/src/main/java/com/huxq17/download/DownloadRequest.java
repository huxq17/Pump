package com.huxq17.download;

public class DownloadRequest {
    private String url;
    private String filePath;
    private int threadNum;

    private DownloadRequest(String url, String filePath, int threadNum) {
        this.url = url;
        this.filePath = filePath;
        this.threadNum = threadNum;
    }

    public static DownloadRequest obtain(TransferInfo info) {
        synchronized (info) {
            return new DownloadRequest(info.getUrl(), info.getFilePath(), info.threadNum);
        }
    }
}
