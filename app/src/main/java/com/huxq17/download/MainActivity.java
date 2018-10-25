package com.huxq17.download;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.huxq17.download.listener.DownloadObserver;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private String url = "http://www.clcxx.com/app/clcxx.apk";
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initProgressDialog();
        progressDialog.show();
        File file3 = new File(getExternalCacheDir().getAbsolutePath(), "download.apk");
        Pump.from(url).into(file3.getAbsolutePath()).subscribe(new DownloadObserver() {
            @Override
            public void onProgressUpdate(int progress) {
                Log.e("main", "Main progress=" + progress);
                progressDialog.setProgress(progress);
                if (progress == 100) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onError(int errorCode) {

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
        progressDialog.setCancelable(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
