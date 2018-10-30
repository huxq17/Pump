package com.huxq17.download.demo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.huxq17.download.DownloadConfig;
import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Pump;
import com.huxq17.download.listener.DownloadObserver;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    //        private String url = "http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h178_1.41.2.16_5a7ef8.apk";
//    private String url = "http://down.youxifan.com/Q6ICeD";
//    private String url = "http://www.anzhi.com/pkg/7083_com.sup.android.superb.html#";
    private String url = "http://xiazai.3733.com/pojie/game/podsctjpjb.apk";
    String pipixiaUrl = "http://gyxzss.syzjxz2018.cn/ss1/rj_limin1/huajingwx.apk";
    private ProgressDialog progressDialog;
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgressUpdate(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            if (downloadInfo.getUrl().equals(pipixiaUrl)) {
                progressDialog.setProgress(progress);
                if (progress == 100) {
                    progressDialog.dismiss();
                }
            }
        }

        @Override
        public void onError(int errorCode) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initProgressDialog();
        DownloadConfig downloadConfig = new DownloadConfig();
        downloadConfig.downloadThreadNumber = 3;
        downloadConfig.maxRunningTaskNumber = 3;
        downloadConfig.forceReDownload = true;
        Pump.setDownloadConfig(downloadConfig);
        Pump.subscribe(downloadObserver);

        //merge 16157 12719 12754
        //merge 13454 14297 14448
        findViewById(R.id.add_task).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File pipixiaFile = new File(getExternalCacheDir().getAbsolutePath(), "pipixia.apk");
                Pump.download(pipixiaUrl, pipixiaFile.getAbsolutePath());
                progressDialog.show();
            }
        });
        findViewById(R.id.jump_download_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final File file1 = new File(getExternalCacheDir().getAbsolutePath(), "download1.apk");
                File file2 = new File(getExternalCacheDir().getAbsolutePath(), "download2.apk");
                File file3 = new File(getExternalCacheDir().getAbsolutePath(), "download3.apk");
                File file4 = new File(getExternalCacheDir().getAbsolutePath(), "download4.apk");
                Pump.download(url, file1.getAbsolutePath());
                Pump.download(url, file2.getAbsolutePath());
                Pump.download(url, file3.getAbsolutePath());
                Pump.download(url, file4.getAbsolutePath());
                startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
            }
        });
    }

    private void initProgressDialog() {
        //创建进度条对话框
        progressDialog = new ProgressDialog(this);
        //设置标题
        progressDialog.setTitle("正在下载");
        //设置信息
        progressDialog.setMessage("玩命下载中...");
        progressDialog.setProgress(0);
        //设置显示的格式
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        Pump.unSubscribe(downloadObserver);
    }


}
