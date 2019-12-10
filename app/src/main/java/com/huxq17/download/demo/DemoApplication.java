package com.huxq17.download.demo;

import android.app.Application;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadTaskExecutor;
import com.huxq17.download.core.SimpleDownloadTaskExecutor;

public class DemoApplication extends Application {
    private static DemoApplication instance;

    public static DemoApplication getInstance() {
        return instance;
    }

    public DownloadTaskExecutor imageDownloadDispatcher = new SimpleDownloadTaskExecutor() {

        @Override
        public int getMaxDownloadNumber() {
            return PumpFactory.getService(IDownloadConfigService.class).getMaxRunningTaskNumber();
        }

        @Override
        public String getName() {
            return "ImageDownloadDispatcher";
        }

        @Override
        public String getTag() {
            return "image";
        }
    };
    public DownloadTaskExecutor musicDownloadDispatcher = new SimpleDownloadTaskExecutor() {

        @Override
        public int getMaxDownloadNumber() {
            return 1;
        }

        @Override
        public String getName() {
            return "MusicDownloadDispatcher";
        }

        @Override
        public String getTag() {
            return "music";
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }
}
