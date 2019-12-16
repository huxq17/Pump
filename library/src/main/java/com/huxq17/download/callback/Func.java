package com.huxq17.download.callback;

import android.support.annotation.NonNull;

public interface Func<R> {
    /**
     *
     * @param result result Result is never null.
     */
    void call(@NonNull R result);
}
