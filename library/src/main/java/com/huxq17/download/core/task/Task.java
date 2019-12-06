package com.huxq17.download.core.task;

public interface Task extends Runnable {
    void cancel();
}
