package com.huxq17.download;

import com.huxq17.download.task.Task;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TaskManager {
    private static final ExecutorService pool = Executors.newCachedThreadPool();

    public static void execute(Task runnable) {
        pool.execute(runnable);
    }
}
