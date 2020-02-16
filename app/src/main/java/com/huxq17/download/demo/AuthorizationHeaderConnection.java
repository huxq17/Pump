package com.huxq17.download.demo;

import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;

import okhttp3.OkHttpClient;

public class AuthorizationHeaderConnection extends OkHttpDownloadConnection {
    public AuthorizationHeaderConnection(OkHttpClient okHttpClient, String url) {
        super(okHttpClient, url);
        // add authorization header here.
//        addHeader("user-agent","");
    }
    public static class Factory implements DownloadConnection.Factory {
        private OkHttpClient okHttpClient;

        public Factory(OkHttpClient okHttpClient) {
            this.okHttpClient = okHttpClient;
        }

        @Override
        public DownloadConnection create(String url) {
            return new AuthorizationHeaderConnection(okHttpClient, url);
        }
    }
}
