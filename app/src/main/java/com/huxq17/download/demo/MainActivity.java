package com.huxq17.download.demo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

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
    private String url2 = "https://file.izuiyou.com/download/package/zuiyou.apk?from=ixiaochuan";
    String url4 = "http://v.nq6.com/xinqu.apk";
    private ProgressDialog progressDialog;
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgress(int progress) {
            progressDialog.setProgress(progress);
        }

        @Override
        public boolean filter(DownloadInfo downloadInfo) {
            String filePath = downloadInfo.getFilePath();
            String url = downloadInfo.getUrl();
            return filePath.endsWith("pipixia.apk");
        }

        @Override
        public void onSuccess() {
            progressDialog.dismiss();
            Toast.makeText(MainActivity.this, "Download Finished", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            progressDialog.dismiss();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initProgressDialog();
        //只要在第一次提交下载任务之前设置就可以。建议在application的onCreate里做
        DownloadConfig.newBuilder()
                //设置下载文件时分配的线程数量，默认是3个
                .setThreadNum(3)
                //设置最多允许同时运行几个下载任务，默认是3个
                .setMaxRunningTaskNum(3)
                //设置是否重复下载已经下载完成了的文件，默认不重复下载
                .setForceReDownload(true)
                .build();
        Pump.subscribe(downloadObserver);

        findViewById(R.id.add_task).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File pipixiaFile = new File(getExternalCacheDir().getAbsolutePath(), "pipixia.apk");
                Pump.download(url4, pipixiaFile.getAbsolutePath());
                progressDialog.setProgress(0);
                progressDialog.show();
            }
        });
        findViewById(R.id.add_download_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file1 = new File(getExternalCacheDir().getAbsolutePath(), "download1.apk");
                File file2 = new File(getExternalCacheDir().getAbsolutePath(), "download2.apk");
                File file3 = new File(getExternalCacheDir().getAbsolutePath(), "download3.apk");
                File file4 = new File(getExternalCacheDir().getAbsolutePath(), "download4.apk");
                Pump.download(url, file1.getAbsolutePath());
                Pump.download(url2, file2.getAbsolutePath());
                Pump.download(url4, file3.getAbsolutePath());
                Pump.download(url4, file4.getAbsolutePath());
            }
        });
        findViewById(R.id.jump_download_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
            }
        });
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading");
//        progressDialog.setMessage("Downloading now...");
        progressDialog.setProgress(0);
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
