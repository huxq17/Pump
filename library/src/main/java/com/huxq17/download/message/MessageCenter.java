package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.buyi.huxq17.serviceagency.ServiceAgency;
import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadDetailsInfo;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.listener.DownloadObserver;
import com.huxq17.download.manager.IDownloadManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;

@ServiceAgent
public class MessageCenter implements IMessageCenter {
    private Context context;
    private ArrayList<WeakReference<DownloadObserver>> observers = new ArrayList<>();
    LinkedHashSet<WeakReference<DownloadObserver>> observersWf = new LinkedHashSet<>();
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
            ArrayList<WeakReference<DownloadObserver>> nullObserverList = null;
            int observerSize = observers.size();
            for (int i = 0; i < observerSize; i++) {
                WeakReference<DownloadObserver> weakReference = observers.get(i);
                DownloadObserver observer = weakReference.get();
                if (observer != null && observer.isEnable()) {
                    if (observer.filter(snapshot.downloadInfo)) {
                        observer.downloading(snapshot);
                    }
                } else {
                    if (nullObserverList == null) {
                        nullObserverList = new ArrayList<>();
                    }
                    nullObserverList.add(weakReference);
                }
            }
            removeNullObserverIfNeed(nullObserverList);
            snapshot.recycle();
        }
    };

    private void removeNullObserverIfNeed(ArrayList<WeakReference<DownloadObserver>> nullObserverList) {
        if (nullObserverList != null) {
            Iterator<WeakReference<DownloadObserver>> iterable = nullObserverList.iterator();
            while (iterable.hasNext()) {
                WeakReference<DownloadObserver> observerWf = iterable.next();
                observers.remove(observerWf);
                iterable.remove();
            }
        }
    }

    @Override
    public void start(Context context) {
        this.context = context;
    }

    private boolean isShutdown() {
        return ServiceAgency.getService(IDownloadManager.class).isShutdown();
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
    public synchronized void register(DownloadObserver downloadObserver) {
        observers.add(new WeakReference<>(downloadObserver));
    }

    @Override
    public synchronized void unRegister(DownloadObserver downloadObserver) {
//        observers.remove(downloadObserver);
    }
}
