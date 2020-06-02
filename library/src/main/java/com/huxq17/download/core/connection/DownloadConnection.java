package com.huxq17.download.core.connection;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;


public interface DownloadConnection {
    void addHeader(String key, String value);

    String getHeader(String key);

    Response connect() throws IOException;

    Response connect(@NonNull String method) throws IOException;

    void prepareDownload(File file) throws IOException;

    int downloadBuffer(byte[] buffer, int offset, int byteCount) throws IOException;

    void flushDownload() throws IOException;

    void close();

    void cancel();

    boolean isCanceled();

    interface Factory {
        DownloadConnection create(@NonNull Request.Builder requestBuilder);
    }
}
