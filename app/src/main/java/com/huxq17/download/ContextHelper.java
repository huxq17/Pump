package com.huxq17.download;

import android.content.Context;

public class ContextHelper {
    private Context mContext;
    private static ContextHelper instance;

    private ContextHelper(Context context) {
        mContext = context;
    }

    public static void init(Context context) {
        instance = new ContextHelper(context);
    }

    public static Context getContext() {
        return instance.mContext;
    }
}
