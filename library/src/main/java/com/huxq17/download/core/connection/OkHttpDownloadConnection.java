package com.huxq17.download.core.connection;

import androidx.annotation.NonNull;

import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class OkHttpDownloadConnection implements DownloadConnection {
    private Response response;
    private Call call;
    private OkHttpClient okHttpClient;
    private BufferedSink bufferedSink;
    private BufferedSource bufferedSource;
    private Request.Builder builder;

    public OkHttpDownloadConnection(OkHttpClient okHttpClient, Request.Builder builder) {
        this.okHttpClient = okHttpClient;
        this.builder = builder;
    }

    @Override
    public void addHeader(String key, String value) {
        builder.addHeader(key, value);
    }

    @Override
    public String getHeader(String key) {
        return response.header(key);
    }

    @Override
    public Response connect() throws IOException {
        call = okHttpClient.newCall(builder.build());
        return response = call.execute();
    }

    @Override
    public Response connect(@NonNull String method) throws IOException {
        Request request = builder.method(method, null).build();
        call = okHttpClient.newCall(request);
        return response = call.execute();
    }

    @Override
    public void prepareDownload(File downloadFile) throws IOException {
        bufferedSource = response.body().source();
        bufferedSink = Okio.buffer(Okio.appendingSink(downloadFile));
    }

    @Override
    public int downloadBuffer(byte[] buffer, int offset, int byteCount) throws IOException {
        int len = bufferedSource.read(buffer, offset, byteCount);
        if (len != -1) {
            bufferedSink.write(buffer, 0, len);
        }
        return len;
    }

    @Override
    public void flushDownload() throws IOException {
        bufferedSink.flush();
    }

    @Override
    public void close() {
        Util.closeQuietly(bufferedSink);
        Util.closeQuietly(bufferedSource);
    }

    @Override
    public void cancel() {
        if (call != null) {
            call.cancel();
        }
    }

    @Override
    public boolean isCanceled() {
        return call != null && call.isCanceled();
    }

    public static class Factory implements DownloadConnection.Factory {
        private OkHttpClient okHttpClient;

        public Factory(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
        }

        @Override
        public DownloadConnection create(@NonNull Request.Builder requestBuilder) {
            return new OkHttpDownloadConnection(okHttpClient, requestBuilder);
        }
    }
}
