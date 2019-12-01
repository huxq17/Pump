package com.huxq17.download.connection;

import com.huxq17.download.Utils.Util;

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
    private BufferedSink bufferedSink ;
    private BufferedSource bufferedSource;
    private Request.Builder builder;
    private String url;

    public OkHttpDownloadConnection(OkHttpClient okHttpClient,String url) {
        this.okHttpClient = okHttpClient;
        builder = new Request.Builder();
        this.url = url;
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
    public void connect() throws IOException {
        Request request = builder.get()
                .url(url)
                .build();
        call = okHttpClient.newCall(request);
        response = call.execute();
    }

    @Override
    public void prepareDownload(File downloadFile) throws IOException {
        bufferedSource = response.body().source();
        bufferedSink = Okio.buffer(Okio.appendingSink(downloadFile));
    }

    @Override
    public int downloadBuffer(byte[] buffer) throws IOException {
        int len = bufferedSource.read(buffer);
        if (len != -1) {
            bufferedSink.write(buffer, 0, len);
        }
        return len;
    }

    @Override
    public void downloadFlush() throws IOException {
        bufferedSink.flush();
    }

    @Override
    public int getResponseCode() {
        return response.code();
    }

    @Override
    public boolean isSuccessful() {
        return response.isSuccessful();
    }

    @Override
    public void close() {
        Util.closeQuietly(bufferedSink);
        Util.closeQuietly(bufferedSource);
        Util.closeQuietly(response);
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
        public DownloadConnection create(String url) {
            return new OkHttpDownloadConnection(okHttpClient,url);
        }
    }
}
