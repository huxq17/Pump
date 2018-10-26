package com.huxq17.download.demo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.huxq17.download.DownloadInfo;
import com.huxq17.download.Pump;
import com.huxq17.download.listener.DownloadObserver;

import java.io.File;

public class MainActivity extends AppCompatActivity {
        private String url = "http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h178_1.41.2.16_5a7ef8.apk";
//    private String url = "http://down.youxifan.com/Q6ICeD";
//    private String url = "http://xiazai.3733.com/pojie/game/podsctjpjb.apk";
    private ProgressDialog progressDialog;
    DownloadObserver downloadObserver = new DownloadObserver() {
        @Override
        public void onProgressUpdate(int progress) {
            DownloadInfo downloadInfo = getDownloadInfo();
            Log.e("main", "Main progress=" + progress + ";filePath=" + downloadInfo.filePath);
//        progressDialog.setProgress(progress);
//        if (progress == 100) {
//            progressDialog.dismiss();
//        }
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
//        progressDialog.show();
        File file1 = new File(getExternalCacheDir().getAbsolutePath(), "download1.apk");
        File file2 = new File(getExternalCacheDir().getAbsolutePath(), "download2.apk");
        File file3 = new File(getExternalCacheDir().getAbsolutePath(), "download3.apk");
        Pump.from(url).into(file1.getAbsolutePath()).subscribe(downloadObserver);
//        Pump.from(url).into(file2.getAbsolutePath()).subscribe(downloadObserver);
//        Pump.from(url).into(file3.getAbsolutePath()).subscribe(downloadObserver);
        //merge 16157 12719 12754
        //merge 13454 14297 14448
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
