package com.huxq17.download.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.manager.IDownloadManager;
import com.huxq17.download.utils.LogUtil;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

public class MessageCenter implements IMessageCenter {
    private Context context;
    private ConcurrentLinkedQueue<DownloadListener> observers = new ConcurrentLinkedQueue<>();
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (isShutdown()) {
                return;
            }
            handleDownloadInfoSnapshot(getObserverIterator(), (DownloadInfo) msg.obj);
        }
    };
    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private CleanupThread cleanupThread;

    static class ListenerWeakReference<M> extends WeakReference<M> {
        final DownloadListener downloadListener;

        ListenerWeakReference(DownloadListener downloadListener, M referent, ReferenceQueue<? super M> q) {
            super(referent, q);
            this.downloadListener = downloadListener;
        }
    }

    @Override
    public void start(Context context) {
        this.context = context;
        CleanupThread cleanupThread = new CleanupThread(referenceQueue);
        cleanupThread.start();
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
        if (observers.add(downloadListener)) {
            WeakReference reference = downloadListener.getReference();
            if (reference != null) {
                ListenerWeakReference<Object> weakReference = new ListenerWeakReference<>(downloadListener, reference.get(), referenceQueue);
            }
        }
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

    private static class CleanupThread extends Thread {
        private final ReferenceQueue<Object> referenceQueue;

        CleanupThread(ReferenceQueue<Object> referenceQueue) {
            this.referenceQueue = referenceQueue;
            setDaemon(true);
            setName("Pump-refQueue");
        }

        @Override
        public void run() {
            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);
            while (true) {
                try {
                    // Prior to Android 5.0, even when there is no local variable, the result from
                    // remove() & obtainMessage() is kept as a stack local variable.
                    // We're forcing this reference to be cleared and replaced by looping every second
                    // when there is nothing to do.
                    // This behavior has been tested and reproduced with heap dumps.
                    ListenerWeakReference<?> remove =
                            (ListenerWeakReference<?>) referenceQueue.remove(1000);
                    if (remove != null) {
                        LogUtil.e("remove downloadListener=" + remove.downloadListener);
                        remove.downloadListener.disable();
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (final Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        void shutdown() {
            interrupt();
        }
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

    Handler getHandler() {
        return handler;
    }

    int getObserverSize() {
        return observers.size();
    }
}
