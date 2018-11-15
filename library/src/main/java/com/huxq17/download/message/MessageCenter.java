package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.DownloadInfoSnapshot;
import com.huxq17.download.TransferInfo;
import com.huxq17.download.listener.DownloadObserver;

import java.util.ArrayList;

@ServiceAgent
public class MessageCenter implements IMessageCenter {
    private Context context;
    private ArrayList<DownloadObserver> observers = new ArrayList<>();
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            long high32Bit = (msg.arg1 & 0x00000000ffffffffL) << 32;
//            int low32Bit = msg.arg2;
//            downloadInfo.snapshotCompletedSize(high32Bit + low32Bit);
            DownloadInfoSnapshot snapshot = (DownloadInfoSnapshot) msg.obj;
            int observerSize = observers.size();
            for (int i = 0; i < observerSize; i++) {
                DownloadObserver observer = observers.get(i);
                if (observer.filter(snapshot.downloadInfo)) {
                    observer.downloading(snapshot);
                }
            }
            snapshot.recycle();
        }
    };

    @Override
    public void start(Context context) {
        this.context = context;
    }

    @Override
    public void notifyProgressChanged(TransferInfo downloadInfo) {
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
        if (!observers.contains(downloadObserver))
            observers.add(downloadObserver);
    }

    @Override
    public synchronized void unRegister(DownloadObserver downloadObserver) {
        observers.remove(downloadObserver);
    }
}
