package com.huxq17.download.core.task;

public abstract class Task implements Runnable {
    protected Thread currentThread;

    @Override
    public final void run() {
        currentThread = Thread.currentThread();
        execute();
        currentThread = null;
    }

    protected abstract void execute();

    protected boolean isCanceled() {
        return currentThread != null && currentThread.isInterrupted();
    }

    public abstract void cancel();
}
