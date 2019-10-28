package com.huxq17.download;

import android.os.Handler;
import android.os.Looper;

import com.huxq17.download.task.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TaskManager {
    private static final ExecutorService pool = Executors.newCachedThreadPool();
    /**
     * UI线程的handler
     */
    private static final Handler mUiHandler = new Handler(Looper.getMainLooper());

    public static void execute(Task runnable) {
        pool.execute(runnable);
    }

    public static void execute(Runnable runnable) {
        pool.execute(runnable);
    }

    public static void executeOnMainThread(Runnable runnable) {
        mUiHandler.post(runnable);
    }
    public static Future<?> submit(Callable task) {
        return pool.submit(task);
    }
    public static Future<?> submit(Runnable task) {
        return pool.submit(task);
    }
}
