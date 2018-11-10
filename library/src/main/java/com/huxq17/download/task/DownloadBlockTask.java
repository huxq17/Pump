package com.huxq17.download.task;


import com.huxq17.download.DownloadBatch;
import com.huxq17.download.ErrorCode;
import com.huxq17.download.OKHttpUtils;
import com.huxq17.download.Utils.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class DownloadBlockTask implements Task {
    private DownloadBatch batch;
    private DownloadTask downloadTask;
    private CountDownLatch countDownLatch;
    private boolean isCanceled;
    private Call call;

    public DownloadBlockTask(DownloadBatch batch, CountDownLatch countDownLatch, DownloadTask downloadTask) {
        this.batch = batch;
        this.downloadTask = downloadTask;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public void run() {
        isCanceled = false;
        long downloadedSize = batch.downloadedSize;
        long startPosition = batch.startPos + downloadedSize;
        long endPosition = batch.endPos;
        File tempFile = batch.tempFile;
        FileOutputStream fileOutputStream = null;
        if (startPosition != endPosition + 1) {
            OkHttpClient okHttpClient = OKHttpUtils.get();
            Request request = new Request.Builder()
                    .get()
                    .addHeader("Range", "bytes=" + startPosition + "-" + endPosition)
//                    .addHeader("Accept-Encoding", "identity")
                    .url(batch.url)
                    .build();
            Response response = null;
            InputStream inputStream = null;
            try {
                call = okHttpClient.newCall(request);
                response = call.execute();
                int code = response.code();
                if (code == 206) {
                    inputStream = response.body().byteStream();
                    byte[] buffer = new byte[8092];
                    int len;
                    //TODO 写入文件的时候可以尝试用MappedByteBuffer共享内存优化。 用okio优化比较下
                    fileOutputStream = new FileOutputStream(tempFile, true);
                    while (!isCanceled && (len = inputStream.read(buffer)) != -1) {
                        if (downloadTask.onDownload(len)) {
                            fileOutputStream.write(buffer, 0, len);
                        } else {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                if (!call.isCanceled()) {
                    e.printStackTrace();
                    downloadTask.setErrorCode(ErrorCode.NETWORK_UNAVAILABLE);
                }
            } finally {
                Util.closeQuietly(inputStream);
                Util.closeQuietly(fileOutputStream);
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
