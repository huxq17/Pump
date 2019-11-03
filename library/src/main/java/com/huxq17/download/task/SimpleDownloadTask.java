package com.huxq17.download.task;


import com.huxq17.download.DownloadChain;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadRequest;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.Utils.Util;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


public class SimpleDownloadTask implements Task {
    private DownloadChain downloadChain;
    private boolean isCanceled;
    private Call call;
    private DownloadRequest downloadRequest;

    public SimpleDownloadTask(DownloadChain downloadChain) {
        this.downloadChain = downloadChain;
        downloadRequest = downloadChain.getDownloadTask().getRequest();
    }

    @Override
    public void run() {
        isCanceled = false;
        DownloadTask downloadTask = downloadChain.getDownloadTask();
        DownloadDetailsInfo downloadInfo = downloadTask.getDownloadInfo();
        OkHttpClient okHttpClient = OKHttpUtils.get();
        File downloadFile = new File(downloadRequest.getFilePath());
        Util.deleteFile(downloadFile);
        Request request = new Request.Builder()
                .get()
                .url(downloadRequest.getUrl())
                .build();
        Response response = null;
        BufferedSink bufferedSink = null;
        BufferedSource bufferedSource = null;
        try {
            if(downloadFile.createNewFile()){
                call = okHttpClient.newCall(request);
                response = call.execute();
                Headers headers = response.headers();
                long contentLength = Util.parseContentLength(headers.get("Content-Length"));
                if (contentLength > 0) {
                    downloadInfo.setContentLength(contentLength);
                }
                if (response.isSuccessful()) {
                    byte[] buffer = new byte[8092];
                    bufferedSource = response.body().source();
                    int len;
                    bufferedSink = Okio.buffer(Okio.appendingSink(downloadFile));
                    while (!isCanceled && (len = bufferedSource.read(buffer)) != -1) {
                        if (downloadTask.onDownload(len)) {
                            bufferedSink.write(buffer, 0, len);
                        } else {
                            break;
                        }
                    }
                    bufferedSink.flush();
                    downloadInfo.setContentLength(downloadFile.length());
                }
            }
        } catch (IOException e) {
            if (!call.isCanceled()) {
                e.printStackTrace();
                downloadTask.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
            }
        } finally {
            Util.closeQuietly(bufferedSink);
            Util.closeQuietly(bufferedSource);
            Util.closeQuietly(response);
        }
    }

    @Override
    public void cancel() {
        isCanceled = true;
        if (call != null) {
            call.cancel();
        }
    }
}
