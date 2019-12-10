package com.huxq17.download.demo;

import android.app.Application;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadDispatcher;

public class DemoApplication extends Application {
    private static DemoApplication instance;
    public static DemoApplication getInstance(){
        return instance;
    }

    public DownloadDispatcher imageDownloadDispatcher = new DownloadDispatcher() {

        @Override
        public int getMaxDownloadNumber() {
            return PumpFactory.getService(IDownloadConfigService.class).getMaxRunningTaskNumber();
        }

        @Override
        public String getName() {
            return "ImageDownloadDispatcher";
        }
    };
    public DownloadDispatcher musicDownloadDispatcher = new DownloadDispatcher() {

        @Override
        public int getMaxDownloadNumber() {
            return 1;
        }

        @Override
        public String getName() {
            return "MusicDownloadDispatcher";
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

    }
}
