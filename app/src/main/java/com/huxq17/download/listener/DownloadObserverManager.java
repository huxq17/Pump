package com.huxq17.download.listener;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.provider.Provider;

import java.util.ArrayList;

@ServiceAgent
public class DownloadObserverManager implements IDownloadObserverManager {
    private Context context;
    private ArrayList<DownloadObserver> observers = new ArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ContentObserver mContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            for (DownloadObserver observer : observers) {
                int progress = observer.calculateDownloadProgress();
                observer.onProgressUpdate(progress);
            }
        }
    };

    @Override
    public void start(Context context) {
        this.context = context;
        context.getContentResolver().registerContentObserver(Provider.CONTENT_URI, true, mContentObserver);
    }

    @Override
    public void register(DownloadObserver downloadObserver) {
        observers.add(downloadObserver);
    }

    @Override
    public void unRegister(DownloadObserver downloadObserver) {
        observers.remove(downloadObserver);
    }
}
