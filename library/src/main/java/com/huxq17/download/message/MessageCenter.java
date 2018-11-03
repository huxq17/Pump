package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
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
            TransferInfo downloadInfo = (TransferInfo) msg.obj;
            int observerSize = observers.size();
            for (int i = 0; i < observerSize; i++) {
                DownloadObserver observer = observers.get(i);
                observer.downloading(downloadInfo);
            }
        }
    };

    @Override
    public void start(Context context) {
        this.context = context;
    }

    @Override
    public void notifyProgressChanged(TransferInfo downloadInfo) {
        handler.removeCallbacksAndMessages(null);
        Message message = Message.obtain();
        message.obj = downloadInfo;
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
