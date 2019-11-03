package com.huxq17.download.task;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.Utils.Util;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


public class DownloadBlockTask implements Task {
    private DownloadBatch batch;
    private DownloadChain downloadChain;
    private CountDownLatch countDownLatch;
    private volatile boolean isCanceled;
    private Call call;

    public DownloadBlockTask(DownloadBatch batch, CountDownLatch countDownLatch, DownloadChain downloadChain) {
        this.batch = batch;
        this.downloadChain = downloadChain;
        this.countDownLatch = countDownLatch;
        isCanceled = false;
    }

    @Override
    public void run() {
        long downloadedSize = batch.downloadedSize;
        long startPosition = batch.startPos + downloadedSize;
        long endPosition = batch.endPos;
        File tempFile = batch.tempFile;
        DownloadTask downloadTask = downloadChain.getDownloadTask();
        if (startPosition != endPosition + 1) {
            OkHttpClient okHttpClient = OKHttpUtils.get();
            Request request = new Request.Builder()
                    .get()
                    .addHeader("Range", "bytes=" + startPosition + "-" + endPosition)
//                    .addHeader("Accept-Encoding", "identity")
                    .url(batch.url)
                    .build();
            Response response = null;
            BufferedSink bufferedSink = null;
            BufferedSource bufferedSource = null;
            try {
                call = okHttpClient.newCall(request);
                response = call.execute();
                int code = response.code();
                if (!isCanceled) {
                    if (code == HttpURLConnection.HTTP_PARTIAL || (response.isSuccessful() && downloadTask.isDowngrade())) {
                        bufferedSource = response.body().source();
                        int len;
                        bufferedSink = Okio.buffer(Okio.appendingSink(tempFile));
                        byte[] buffer = new byte[8092];
                        while (!isCanceled && (len = bufferedSource.read(buffer)) != -1) {
                            if (!downloadTask.onDownload(len)) {
                                break;
                            }
                            bufferedSink.write(buffer, 0, len);
                        }
                        bufferedSink.flush();
                    } else if (code == HttpURLConnection.HTTP_OK) {
                        //downgrade when server not support breakpoint download.
                        downloadChain.downgrade();
                    }
                }
            } catch (IOException e) {
                if (!call.isCanceled()) {
                    e.printStackTrace();
                    downloadTask.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
                    downloadTask.cancel(!downloadChain.isRetryable());
                }
            } finally {
                Util.closeQuietly(bufferedSource);
                Util.closeQuietly(bufferedSink);
                Util.closeQuietly(response);
            }
        }
        countDownLatch.countDown();
        this.batch = null;
        this.downloadChain = null;
        this.countDownLatch = null;
    }

    @Override
    public void cancel() {
        isCanceled = true;
        if (call != null) {
            call.cancel();
        }
    }
}
