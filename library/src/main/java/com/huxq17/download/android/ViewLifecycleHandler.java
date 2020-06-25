package com.huxq17.download.android;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import com.huxq17.download.core.DownloadListener;
import com.huxq17.download.utils.LogUtil;

public class ViewLifecycleHandler {
    public static void handleLifecycle(Lifecycle lifecycle, DownloadListener downloadListener) {
        if (!lifecycle.getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            LogUtil.w(lifecycle + " is "+lifecycle.getCurrentState()+", so disable " + downloadListener);
            downloadListener.disable();
            return;
        }
        lifecycle.addObserver(new DownloadListenerObserver(downloadListener));
    }

    public static void handleLifecycleForFragment(Fragment fragment, Lifecycle lifecycle,
                                                  DownloadListener downloadListener) {
        if (!lifecycle.getCurrentState().isAtLeast(Lifecycle.State.INITIALIZED)) {
            LogUtil.w(lifecycle + " is "+lifecycle.getCurrentState()+", so disable " + downloadListener);
            downloadListener.disable();
            return;
        }
        if (lifecycle.getCurrentState().isAtLeast(Lifecycle.State.STARTED)) {
            fragment.getViewLifecycleOwner().getLifecycle().addObserver(new DownloadListenerObserver(downloadListener));
        } else {
            lifecycle.addObserver(new DownloadListenerObserver(downloadListener, fragment));
        }

    }

    private static class DownloadListenerObserver implements LifecycleEventObserver {
        private DownloadListener downloadListener;
        private Fragment fragment;

        private DownloadListenerObserver(DownloadListener downloadListener) {
            this.downloadListener = downloadListener;
        }

        private DownloadListenerObserver(DownloadListener downloadListener, Fragment fragment) {
            this.downloadListener = downloadListener;
            this.fragment = fragment;
        }

        private void onStart() {
            if (fragment != null) {
                fragment.getLifecycle().removeObserver(this);
                fragment.getViewLifecycleOwner().getLifecycle().addObserver(this);
                fragment = null;
            }
        }

        private void onDestroy() {
            LogUtil.e("disable " + downloadListener);
            downloadListener.disable();
        }

        @Override
        public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
            switch (event) {
                case ON_START:
                    onStart();
                    break;
                case ON_DESTROY:
                    onDestroy();
                    break;
            }
        }
    }
}
