package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.webkit.URLUtil;

import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.PumpFactory;
import com.huxq17.download.Utils.LogUtil;
import com.huxq17.download.manager.IDownloadManager;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageCenter implements IMessageCenter {
    private ConcurrentLinkedQueue<DownloadListener> observers = new ConcurrentLinkedQueue<>();

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
            snapshot.recycle();
        }
    };

    @Override
    public void start(Context context) {
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
    public synchronized void unRegister(String id) {
        int beforeSize = observers.size();
        Iterator<DownloadListener> iterator = observers.iterator();
        while (iterator.hasNext()) {
            DownloadListener downloadListener = iterator.next();
            if (id.equals(downloadListener.getId())) {
                downloadListener.setEnable(false);
                iterator.remove();
            }
        }
        LogUtil.d("unRegister id=" + id + ";size=" + observers.size() + ";before.size=" + beforeSize);
    }

    @Override
    public synchronized void unRegister(DownloadListener downloadListener) {
        downloadListener.setEnable(false);
        int beforeSize = observers.size();
        observers.remove(downloadListener);
        LogUtil.d("unRegister id=" + downloadListener.getId() + ";size=" + observers.size() + ";before.size=" + beforeSize);
    }
}