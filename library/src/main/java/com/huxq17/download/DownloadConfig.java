package com.huxq17.download;

public class DownloadConfig {
    /**
     * 下载单个文件时开启的线程数量
     */
    public int downloadThreadNumber = 3;
    /**
     * 允许同时下载的任务数量
     */
    public int maxRunningTaskNumber = 3;
    /**
     * 是否重复下载已经下载完成了的文件
     */
    public boolean forceReDownload = false;
}
