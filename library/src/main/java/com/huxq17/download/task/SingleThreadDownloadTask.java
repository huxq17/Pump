package com.huxq17.download.task;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.DownloadChain;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.Utils.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;


public class SingleThreadDownloadTask implements Task {
    private DownloadBatch batch;
    private DownloadChain downloadChain;
    private CountDownLatch countDownLatch;
    private boolean isCanceled;
    private Call call;

    public SingleThreadDownloadTask(DownloadBatch batch, CountDownLatch countDownLatch, DownloadChain downloadChain) {
        this.batch = batch;
        this.downloadChain = downloadChain;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        isCanceled = false;
        long downloadedSize = batch.downloadedSize;
        long startPosition = batch.startPos + downloadedSize;
        long endPosition = batch.endPos;
        File tempFile = batch.tempFile;
        DownloadTask downloadTask = downloadChain.getDownloadTask();
        if (startPosition != endPosition + 1) {
            OkHttpClient okHttpClient = OKHttpUtils.get();
            Request request = new Request.Builder()
                    .get()
                    .url(batch.url)
                    .build();
            Response response = null;
            BufferedSink bufferedSink = null;
            BufferedSource bufferedSource = null;
            try {
                call = okHttpClient.newCall(request);
                response = call.execute();
                int code = response.code();
                LogUtil.e("download block code=" + code);
                if (code == 200) {
                    byte[] buffer = new byte[8092];
                    //TODO 写入文件的时候可以尝试用MappedByteBuffer共享内存优化。 用okio优化比较下
                    bufferedSource = response.body().source();
                    int len;
                    bufferedSink = Okio.buffer(Okio.appendingSink(tempFile));
                    while (!isCanceled && (len = bufferedSource.read(buffer)) != -1) {
                        if (downloadTask.onDownload(len)) {
                            bufferedSink.write(buffer, 0, len);
                        } else {
                            break;
                        }
                    }
                    bufferedSink.flush();
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
        countDownLatch.countDown();
    }

    @Override
    public void cancel() {
        isCanceled = true;
        if (call != null) {
            call.cancel();
        }
    }
}
