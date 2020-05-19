package com.huxq17.download.android;

import android.app.Activity;
import android.app.Application;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.huxq17.download.core.DownloadListener;

import java.util.HashMap;
import java.util.Map;

public class ViewLifecycleHandler {
    private static final ViewHandlerManager sViewHandlerManager = new ViewHandlerManager();

    public static void handleLifecycleForActivity(FragmentActivity view, DownloadListener downloadListener) {
        sViewHandlerManager.handleLifecycleForActivity(view, downloadListener);
    }

    public static void handleLifecycleForFragment(Fragment view, DownloadListener downloadListener) {
        sViewHandlerManager.handleLifecycleForFragment(view, downloadListener);
    }

    private static class ViewHandlerManager {
        private boolean mActivityCallbacksIsAdded = false;
        private Map<String, DownloadListener> downloadListenerMap = new HashMap<>();

        private void handleLifecycleForActivity(FragmentActivity activity, DownloadListener downloadListener) {
            if (activity.getFragmentManager() == null || activity.getFragmentManager().isDestroyed()) {
                downloadListener.disable();
                return;
            }
            downloadListenerMap.put(activity.getClass().getSimpleName(), downloadListener);
            if (!mActivityCallbacksIsAdded) {
                mActivityCallbacksIsAdded = true;
                activity.getApplication().registerActivityLifecycleCallbacks(mActivityCallbacks);
            }
        }

        private void handleLifecycleForFragment(Fragment fragment, DownloadListener downloadListener) {
            if (fragment.getFragmentManager() == null || fragment.getFragmentManager().isDestroyed()) {
                downloadListener.disable();
                return;
            }
            downloadListenerMap.put(fragment.getClass().getSimpleName(), downloadListener);
            fragment.getFragmentManager()
                    .registerFragmentLifecycleCallbacks(mParentDestroyedCallback, false);
        }

        private Application.ActivityLifecycleCallbacks mActivityCallbacks =
                new EmptyActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityDestroyed(Activity activity) {
                        DownloadListener downloadListener = downloadListenerMap
                                .remove(activity.getClass().getSimpleName());
                        if (downloadListener != null) {
                            downloadListener.disable();
                        }
                    }
                };

        private FragmentManager.FragmentLifecycleCallbacks mParentDestroyedCallback =
                new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentDestroyed(FragmentManager fm, Fragment parentFragment) {
                        super.onFragmentDestroyed(fm, parentFragment);
                        DownloadListener downloadListener = downloadListenerMap
                                .remove(parentFragment.getClass().getSimpleName());
                        if (downloadListener != null) {
                            downloadListener.disable();
                        }
                    }
                };
    }
}
