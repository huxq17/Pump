package com.huxq17.download.demo;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import okhttp3.internal.Util;

public class Downloader {
    private String url;
    private String filePath;
    private DownloadListener listener;
    private static final Downloader instance = new Downloader();

    public static Downloader getInstance() {
        return instance;
    }

    public Downloader from(String url) {
        this.url = url;
        return this;
    }

    public Downloader into(String filePath) {
        this.filePath = filePath;
        return this;
    }

    public void listener(DownloadListener listener) {
        this.listener = listener;
        download();
    }

    private void download() {
        DownloadRunner runner = new DownloadRunner();
        runner.config(url, filePath, listener);
        new Thread(runner).start();
    }

    public interface DownloadListener {
        void downloading(int progress);

        void success(File file);

        void failed();
    }

    public static class DownloadRunner implements Runnable {
        private String url;
        private String filePath;
        private DownloadListener listener;
        private static final int LISTEN_ING = 1;
        private static final int LISTEN_SUCCESS = 2;
        private static final int LISTEN_FAILED = 3;
        private Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                switch (what) {
                    case LISTEN_ING:
                        int progress = msg.arg1;
                        listener.downloading(progress);
                        break;
                    case LISTEN_SUCCESS:
                        listener.success((File) msg.obj);
                        break;
                    case LISTEN_FAILED:
                        listener.failed();
                        break;
                }
            }
        };

        public void config(String url, String filePath, DownloadListener listener) {
            this.url = url;
            this.filePath = filePath;
            this.listener = listener;
        }

        @Override
        public void run() {
            FileOutputStream fos = null;
            InputStream inputstream = null;
            try {
                URL url = new URL(this.url);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(60000);
                conn.setReadTimeout(60000);
                int sum = 0;
                final long total = conn.getContentLength();
                if (conn.getResponseCode() == 200) {
                    inputstream = conn.getInputStream();
                    byte[] buffer = new byte[8092];
                    int len;
                    File file = new File(filePath);
                    if (!file.getParentFile().exists()) file.mkdirs();
                    if (file.exists()) file.delete();
                    file.createNewFile();
                    fos = new FileOutputStream(file);
                    int lastProgress = 0;
                    while ((len = inputstream.read(buffer)) != -1) {
                        sum += len;
                        fos.write(buffer,0,len);
                        int progress = (int) (sum * 1f / total * 100);
                        if (lastProgress != progress) {
                            Message message = Message.obtain();
                            message.what = LISTEN_ING;
                            message.arg1 = progress;
                            handler.sendMessage(message);
                            lastProgress = progress;
                        }
                    }
                    if (sum == total) {
                        fos.flush();
                        Message message = Message.obtain();
                        message.what = LISTEN_SUCCESS;
                        message.obj = file;
                        handler.sendMessage(message);
                    } else {
                        Message message = Message.obtain();
                        message.what = LISTEN_FAILED;
                        handler.sendMessage(message);
                    }
                }
                conn.disconnect();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = LISTEN_FAILED;
                handler.sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = LISTEN_FAILED;
                handler.sendMessage(message);
            } finally {
                Util.closeQuietly(inputstream);
                Util.closeQuietly(fos);
            }
        }
    }
}
