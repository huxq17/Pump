package com.huxq17.download;

import android.content.Context;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;

public class OKHttpUtils {
    private static  OkHttpClient OK_HTTP_CLIENT;

    public static void init(Context context) {
        File httpCacheDir = new File(context.getCacheDir(), "http");
        long httpCacheSize = 50 * 1024 * 1024;
        Cache cache = new Cache(httpCacheDir,httpCacheSize);
        OK_HTTP_CLIENT = new OkHttpClient().newBuilder()
//                .cache(cache)
                .followRedirects(true)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .writeTimeout(30,TimeUnit.SECONDS)
                .readTimeout(30,TimeUnit.SECONDS)
                .connectTimeout(30,TimeUnit.SECONDS)
                .build();
    }

    public static OkHttpClient get() {
        return OK_HTTP_CLIENT;
    }
}
