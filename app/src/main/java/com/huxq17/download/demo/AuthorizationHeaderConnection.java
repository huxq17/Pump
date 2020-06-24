package com.huxq17.download.demo;

import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AuthorizationHeaderConnection extends OkHttpDownloadConnection {
    public AuthorizationHeaderConnection(OkHttpClient okHttpClient, Request.Builder requestBuilder) {
        super(okHttpClient, requestBuilder);
        // add authorization header here.
        addHeader("User-Agent","Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Safari/537.36");
    }
    public static class Factory implements DownloadConnection.Factory {
        private OkHttpClient okHttpClient;

        public Factory(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
        }


        @Override
        public DownloadConnection create(Request.Builder requestBuilder) {
            return  new AuthorizationHeaderConnection(okHttpClient, requestBuilder);
        }
    }
}
