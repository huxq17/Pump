package com.huxq17.download.core.task;

public abstract class Task implements Runnable {
    protected Thread currentThread;
    private volatile boolean isFinished = false;


    public void waitUntilFinished() {
        while (!isFinished) {
            try {
                synchronized (this) {
                    wait();
                }
            } catch (InterruptedException ignore) {
            }
        }
    }

    @Override
    public final void run() {
        currentThread = Thread.currentThread();
        if (!isCanceled()) {
            execute();
        }
        currentThread = null;

        isFinished = true;
        synchronized (this) {
            notify();
        }
    }

    public boolean isAlive() {
        return currentThread != null && currentThread.isAlive();
    }

    protected abstract void execute();

    protected boolean isCanceled() {
        return currentThread != null && currentThread.isInterrupted();
    }

    public abstract void cancel();
}
