package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.manager.IDownloadManager;

import java.util.Iterator;
import java.util.LinkedHashSet;

public class MessageCenter implements IMessageCenter {
    private Context context;
    private boolean isBusying = false;
    private LinkedHashSet<DownloadListener> observers = new LinkedHashSet<>();
    private LinkedHashSet<DownloadListener> removedObservers = new LinkedHashSet<>();
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isShutdown()) {
                return;
            }
//            long high32Bit = (msg.arg1 & 0x00000000ffffffffL) << 32;
//            int low32Bit = msg.arg2;
//            downloadInfo.snapshotCompletedSize(high32Bit + low32Bit);
            DownloadInfoSnapshot snapshot = (DownloadInfoSnapshot) msg.obj;
            Iterator<DownloadListener> iterator = observers.iterator();
            isBusying = true;
            while (iterator.hasNext()) {
                DownloadListener downloadListener = iterator.next();
                if (downloadListener != null && downloadListener.isEnable()) {
                    if (downloadListener.filter(snapshot.downloadInfo)) {
                        downloadListener.downloading(snapshot);
                    }
                } else {
                    iterator.remove();
                }
            }
            isBusying = false;
            if (removedObservers.size() > 0) {
                observers.removeAll(removedObservers);
                removedObservers.clear();
            }
            snapshot.recycle();
        }
    };

    @Override
    public void start(Context context) {
        this.context = context;
    }

    private boolean isShutdown() {
        return PumpFactory.getService(IDownloadManager.class).isShutdown();
    }

    @Override
    public void notifyProgressChanged(DownloadDetailsInfo downloadInfo) {
        if (isShutdown()) {
            return;
        }
        Message message = Message.obtain();
        DownloadInfoSnapshot snapshot = DownloadInfoSnapshot.obtain();
        message.obj = snapshot;
        snapshot.downloadInfo = downloadInfo;
        snapshot.completedSize = downloadInfo.getCompletedSize();
        snapshot.status = downloadInfo.getStatus();
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
    public synchronized void unRegister(String url) {
        DownloadListener downloadObserver = new DownloadListener(url);
        unRegister(downloadObserver);
    }

    @Override
    public synchronized void unRegister(DownloadListener downloadListener) {
        downloadListener.setEnable(false);
        if (!isBusying) {
            observers.remove(downloadListener);
        } else {
            removedObservers.add(downloadListener);
        }
    }
}
