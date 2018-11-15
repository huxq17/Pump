package com.huxq17.download.action;

import com.huxq17.download.DownloadChain;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.task.DownloadTask;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GetContentLengthAction implements Action {
    @Override
    public boolean proceed(DownloadChain chain) {
        boolean result = true;
        DownloadTask downloadTask = chain.getDownloadTask();
        TransferInfo transferInfo = downloadTask.getDownloadInfo();
        OkHttpClient okHttpClient = OKHttpUtils.get();
        Request request = new Request.Builder()
                .get()
                .addHeader("Accept-Encoding", "identity")
                .url(transferInfo.getUrl())
                .build();
        Response response;
        try {
            response = okHttpClient.newCall(request).execute();
            if (response.cacheResponse() != null) {
                //TODO start download by singleThreadTask
                transferInfo.setContentLength(response.body().contentLength());
            } else {
                if (response.isSuccessful()) {
                    transferInfo.setContentLength(response.body().contentLength());
                } else {
                    int responseCode = response.code();
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        transferInfo.setErrorCode(ErrorCode.FILE_NOT_FOUND);
                    } else {
                        transferInfo.setErrorCode(ErrorCode.UNKNOWN_SERVER_ERROR);
                    }
                    result = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            transferInfo.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            result = false;
        }
        return result;
    }
}
