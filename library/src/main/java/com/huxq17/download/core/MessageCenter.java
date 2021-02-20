package com.huxq17.download.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.core.service.IMessageCenter;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageCenter implements IMessageCenter {
    private Context context;
    private final ConcurrentLinkedQueue<DownloadListener> observers = new ConcurrentLinkedQueue<>();
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (isShutdown()) {
                return;
            }
            handleDownloadInfoSnapshot(getObserverIterator(), (DownloadInfo) msg.obj);
        }
    };

    @Override
    public void start(Context context) {
        this.context = context;
    }

    Iterator<DownloadListener> getObserverIterator() {
        return observers.iterator();
    }

    void handleDownloadInfoSnapshot(Iterator<DownloadListener> iterator, DownloadInfo snapshot) {
        while (iterator.hasNext()) {
            DownloadListener downloadListener = iterator.next();
            if (downloadListener != null && downloadListener.isEnable()) {
                if (downloadListener.filter(snapshot)) {
                    downloadListener.downloading(snapshot);
                }
            } else {
                iterator.remove();
            }
        }
    }

    boolean isShutdown() {
        return PumpFactory.getService(IDownloadManager.class).isShutdown();
    }

    @Override
    public void notifyProgressChanged(DownloadDetailsInfo downloadInfo) {
        if (isShutdown()) {
            return;
        }
        Message message = Message.obtain();
        message.obj = downloadInfo.snapshot();
//        DownloadInfoSnapshot snapshot = DownloadInfoSnapshot.obtain();
//        message.obj = snapshot;
//        snapshot.downloadInfo = downloadInfo;
//        snapshot.completedSize = downloadInfo.getCompletedSize();
//        snapshot.status = downloadInfo.getStatus();

//        long completedSize = downloadInfo.getCompletedSize();
//        //用arg1存高32位的long值
//        message.arg1 = (int) ((completedSize & 0xffffffff00000000L) >> 32);
//        //用arg2存低32位的long值
//        message.arg2 = (int) (completedSize & 0x00000000ffffffffL);
        handler.sendMessage(message);
//        context.getContentResolver().notifyChange(CONTENT_URI, null);
    }

    @Override
    public synchronized void register(DownloadListener downloadListener) {
        downloadListener.setEnable(true);
        observers.add(downloadListener);
    }

    @Override
    public synchronized void unRegister(String id) {
        Iterator<DownloadListener> iterator = observers.iterator();
        while (iterator.hasNext()) {
            DownloadListener downloadListener = iterator.next();
            if (id.equals(downloadListener.getId())) {
                downloadListener.setEnable(false);
                iterator.remove();
            }
        }
    }

    @Override
    public synchronized void unRegister(DownloadListener downloadListener) {
        downloadListener.setEnable(false);
        observers.remove(downloadListener);
    }

    /**
     * Add for test
     */
    Context getContext() {
        return context;
    }

    void setHandler(Handler handler) {
        this.handler = handler;
    }
    Handler getHandler(){
        return handler;
    }

    int getObserverSize() {
        return observers.size();
    }
}
