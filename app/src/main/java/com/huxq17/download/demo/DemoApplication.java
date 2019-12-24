package com.huxq17.download.demo;

import android.app.Application;
import android.text.TextUtils;

import com.huxq17.download.PumpFactory;
import com.huxq17.download.config.DownloadConfig;
import com.huxq17.download.config.IDownloadConfigService;
import com.huxq17.download.core.DownloadInfo;
import com.huxq17.download.core.DownloadInterceptor;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.core.DownloadTaskExecutor;
import com.huxq17.download.core.SimpleDownloadTaskExecutor;
import com.huxq17.download.core.connection.OkHttpDownloadConnection;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.LogUtil;
import com.huxq17.download.utils.OKHttpUtil;

import java.io.File;

public class DemoApplication extends Application {
    private static DemoApplication instance;
    public static final int MD5_VERIFY_FAILED = 3000;

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
        DownloadConfig.newBuilder()
                //Optional,set the maximum number of tasks to run at the same time, default 3.
                .setMaxRunningTaskNum(2)
                //Optional,set the minimum available storage space size for downloading to avoid insufficient storage space during downloading, default is 4kb.
                .setMinUsableStorageSpace(4 * 1024L)
                .setDownloadConnectionFactory(new OkHttpDownloadConnection.Factory(OKHttpUtil.get()))//Optional
                .addDownloadInterceptor(new DownloadInterceptor() {
                    @Override
                    public DownloadInfo intercept(DownloadChain chain) {
                        DownloadRequest downloadRequest = chain.request();
                        DownloadInfo downloadInfo = chain.proceed(downloadRequest);
                        if (downloadInfo.isFinished()) {
                            //verify md5.
                            File downloadFile = new File(downloadInfo.getFilePath());
                            String fileMD5 = Utils.getMD5ByBase64(downloadFile);
                            String serverMD5 = downloadInfo.getMD5();
                            LogUtil.e("fileMD5=" + fileMD5 + ";serverMD5=" + serverMD5);
                            if (!TextUtils.isEmpty(serverMD5) && !serverMD5.equals(fileMD5)) {
                                //setErrorCode will make download failed.
                                downloadInfo.setErrorCode(MD5_VERIFY_FAILED);
                                FileUtil.deleteFile(downloadFile);
                            }

                            //unzip file here if need.
                        }
                        return downloadInfo;
                    }
                })
                .build();

    }
}
