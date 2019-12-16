package com.huxq17.download.demo.remote;

import com.huxq17.download.core.DownloadInfo;

class Music {
    public String url;
    public String id;
    public String name;
    public DownloadInfo downloadInfo;

    public Music(String url, String name) {
        this.url = url;
        this.id = url;
        this.name = name;
    }
}
