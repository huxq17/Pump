package com.huxq17.download.callback;

import android.support.annotation.NonNull;

/**
 * Author: huxiaoqian
 * Version V1.0
 * Date: 2019/10/22
 * Description:
 * Modification History:
 * Date Author Version Description
 * -----------------------------------------------------------------------------------
 * 2019/10/22 huxiaoqian 1.0
 * Why & What is modified:
 */
public interface Func<R> {
    /**
     *
     * @param result result Result is never null.
     */
    void call(@NonNull R result);
}
