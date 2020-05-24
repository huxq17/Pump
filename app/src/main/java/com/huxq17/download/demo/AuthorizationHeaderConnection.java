package com.huxq17.download.demo;

import com.huxq17.download.core.connection.DownloadConnection;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class AuthorizationHeaderConnection extends OkHttpDownloadConnection {
    public AuthorizationHeaderConnection(OkHttpClient okHttpClient, Request.Builder requestBuilder) {
        super(okHttpClient, requestBuilder);
        // add authorization header here.
//        addHeader("user-agent","");
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
