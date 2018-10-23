package com.huxq17.download.action;

public interface Action<T, R> {
    R proceed(T t);
}
