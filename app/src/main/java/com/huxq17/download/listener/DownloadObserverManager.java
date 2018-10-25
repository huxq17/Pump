package com.huxq17.download.listener;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;

import com.buyi.huxq17.serviceagency.annotation.ServiceAgent;
import com.huxq17.download.Utils.Util;
import com.huxq17.download.provider.Provider;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

@ServiceAgent
public class DownloadObserverManager implements IDownloadObserverManager {
    private Context context;
    private CopyOnWriteArrayList<DownloadObserver> observers = new CopyOnWriteArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ContentObserver mContentObserver = new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            for (DownloadObserver observer : observers) {
                String filePath = observer.getObservablePath();
                File file = new File(filePath);
                if (file.isFile()) {
                    File tempDir = Util.getTempDir(filePath);
                    File[] partFiles = tempDir.listFiles();
                    if (partFiles == null) return;
                    int completedSize = 0;
                    for (int i = 0; i < partFiles.length; i++) {
                        completedSize += partFiles[i].length();
                    }
                    int progress = (int) (completedSize * 1f / observer.getDownloadInfo().contentLength * 100);
                    observer.onProgressUpdate(progress);
                } else if (file.isDirectory()) {
                    //TODO 下载组监听

                }
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
