package com.huxq17.download.android;

import android.app.Activity;
import android.app.Application;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.core.DownloadListener;
import com.huxq17.download.utils.LogUtil;

import java.util.HashMap;
import java.util.Map;

public class ViewLifecycleHandler {
    private static final ViewHandlerManager sViewHandlerManager = new ViewHandlerManager();

    public static void handleLifecycleForView(Object view, DownloadListener downloadListener) {
        if(view instanceof FragmentActivity){
            sViewHandlerManager.handleLifecycleForActivity((FragmentActivity) view, downloadListener);
        }else if(view instanceof Fragment){
            sViewHandlerManager.handleLifecycleForFragment((Fragment) view, downloadListener);
        }
    }

    private static class ViewHandlerManager {
        private boolean mActivityCallbacksIsAdded = false;
        private Map<String, DownloadListener> downloadListenerMap = new HashMap<>();

        private void handleLifecycleForActivity(FragmentActivity activity, DownloadListener downloadListener) {
            if (activity.getFragmentManager() == null || activity.getFragmentManager().isDestroyed()) {
                downloadListener.disable();
                LogUtil.w("FragmentActivity "+activity +" 's fragmentManager is null!");
                return;
            }
            downloadListenerMap.put(activity.getClass().getSimpleName(), downloadListener);
            if (!mActivityCallbacksIsAdded) {
                mActivityCallbacksIsAdded = true;
                ((Application) DownloadProvider.context).registerActivityLifecycleCallbacks(mActivityCallbacks);
            }
        }

        private void handleLifecycleForFragment(Fragment fragment, DownloadListener downloadListener) {
            if (fragment.getFragmentManager() == null || fragment.getFragmentManager().isDestroyed()) {
                downloadListener.disable();
                LogUtil.w("fragment "+fragment +" 's fragmentManager is null!");
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
