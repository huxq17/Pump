package com.huxq17.download.demo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.huxq17.download.demo.installapk.APK;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    //        private String url = "http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h178_1.41.2.16_5a7ef8.apk";
//    private String url = "http://down.youxifan.com/Q6ICeD";
//    private String url = "http://www.anzhi.com/pkg/7083_com.sup.android.superb.html#";
    private String url = "http://xiazai.3733.com/pojie/game/podsctjpjb.apk";
    private String url2 = "https://file.izuiyou.com/download/package/zuiyou.apk?from=ixiaochuan";
    String url4 = "http://v.nq6.com/xinqu.apk";
    String url5 = "http://wap.apk.anzhi.com/data4/apk/201810/24/e2cd3e0aded695c8fb7edcc508e3fd1b_37132000.apk";
    private ProgressDialog progressDialog;
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgress(int progress) {
            progressDialog.setProgress(progress);
        }

        @Override
        public boolean filter(DownloadInfo downloadInfo) {
            String url = downloadInfo.getUrl();
            return url.equals(url5);
        }

        @Override
        public void onSuccess() {
            progressDialog.dismiss();
            String apkPath = getDownloadInfo().getFilePath();
            APK.with(MainActivity.this)
                    .from(apkPath)
//                    .forceInstall();
                    .install();
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
        DownloadConfig.newBuilder(getApplicationContext())
                //Set the maximum number of tasks to run, default 3.
                .setMaxRunningTaskNum(3)
                .build();
//        Pump.subscribe(downloadObserver);
//        try {
//            File httpCacheDir = new File(getCacheDir(), "http");
//            long httpCacheSize = 50 * 1024 * 1024;
//            Class.forName("android.net.http.HttpResponseCache")
//                    .getMethod("install", File.class, long.class)
//                    .invoke(null, httpCacheDir, httpCacheSize);
//        } catch (Exception httpResponseCacheNotAvailable) {
//        }
        findViewById(R.id.add_task).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.setProgress(0);
                progressDialog.show();
                File pipixiaFile = new File(getExternalCacheDir().getAbsolutePath(), "pipixia.apk");
                Pump.newRequest(url5, pipixiaFile.getAbsolutePath())
                        //Optionally,Set whether to repeatedly download the downloaded file,default false.
                        .forceReDownload(true)
                        //Optionally,Set how many threads are used when downloading,default 3.
                        .threadNum(3)
                        .submit();
            }
        });

        findViewById(R.id.add_download_list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file1 = new File(getExternalCacheDir().getAbsolutePath(), "download1.apk");
                File file2 = new File(getExternalCacheDir().getAbsolutePath(), "download2.apk");
                File file3 = new File(getExternalCacheDir().getAbsolutePath(), "download3.apk");
                File file4 = new File(getExternalCacheDir().getAbsolutePath(), "download4.apk");
                Pump.newRequest(url, file1.getAbsolutePath())
                        .submit();
                Pump.newRequest(url2, file2.getAbsolutePath())
                        .submit();
                Pump.newRequest(url4, file3.getAbsolutePath())
                        .submit();
                Pump.newRequest(url5, file4.getAbsolutePath())
                        .submit();
//                        File pipixiaFile = new File(getExternalCacheDir().getAbsolutePath(), "pipixia.apk");
//                        Pump.download(url5, pipixiaFile.getAbsolutePath());
            }
        });

        findViewById(R.id.jump_download_list).
                setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(MainActivity.this, DownloadListActivity.class));
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Enable this Observer.
        downloadObserver.enable();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Optionally,disable this observer and Pump will remove this observer later.
        downloadObserver.disable();
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
        //shutdown will stop all tasks and release some resource.
        Pump.shutdown();
    }
}
