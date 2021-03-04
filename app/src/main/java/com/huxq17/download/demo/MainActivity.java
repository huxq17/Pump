package com.huxq17.download.demo;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.huxq17.download.Pump;
import com.huxq17.download.core.DownloadListener;
import com.huxq17.download.core.DownloadRequest;
import com.huxq17.download.demo.installapk.APK;
import com.huxq17.download.demo.remote.RemoteDownloadListActivity;
import com.huxq17.download.utils.LogUtil;

import okhttp3.Request;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity {
    //        private String url = "http://dlied5.myapp.com/myapp/1104466820/sgame/2017_com.tencent.tmgp.sgame_h178_1.41.2.16_5a7ef8.apk";
    private String url = "http://down.youxifan.com/Q6ICeD";
    //    private String url = "http://www.anzhi.com/dl_app.php?s=3080740&n=5";
    //    private String url = "http://xiazai.3733.com/pojie/game/podsctjpjb.apk";
    private String url2 = "https://file.izuiyou.com/download/package/zuiyou.apk?from=ixiaochuan";
    //http://www.httpwatch.com/httpgallery/chunked/chunkedimage.aspx
    String url4 = "http://v.nq6.com/xinqu.apk";
    //    String url5 = "http://t2.hddhhn.com/uploads/tu/201612/98/st93.png";
    String url5 = "http://wdj.anzhi.com/dl_app.php?s=2397021&channel=wandoujia";
    private ProgressDialog progressDialog;
    private final static String TAG = "groupA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initProgressDialog();
//        Pump.subscribe(downloadListener);
//        try {
//            File httpCacheDir = new File(getCacheDir(), "http");
//            long httpCacheSize = 50 * 1024 * 1024;
//            Class.forName("android.net.http.HttpResponseCache")
//                    .getMethod("install", File.class, long.class)
//                    .invoke(null, httpCacheDir, httpCacheSize);
//        } catch (Exception httpResponseCacheNotAvailable) {
//        }
        final EditText etDownload = findViewById(R.id.etDownload);
        findViewById(R.id.add_task).setOnClickListener(v -> {
            progressDialog.setProgress(0);
            progressDialog.show();
            String downloadUrl = etDownload.getText().toString();
            if (downloadUrl.isEmpty()) {
                downloadUrl = url5;
            }

            Pump.newRequest(downloadUrl)
                    .setRequestBuilder(new Request.Builder())
                    .listener(new DownloadListener(MainActivity.this) {

                        @Override
                        public void onProgress(int progress) {
                            progressDialog.setProgress(progress);
                        }

                        @Override
                        public void onSuccess() {
                            progressDialog.dismiss();
                            String apkPath = getDownloadInfo().getFilePath();
                            APK.with(MainActivity.this)
                                    .from(apkPath)
//                                        .forceInstall();
                                    .install();
                            Toast.makeText(MainActivity.this, "Download Finished", Toast.LENGTH_SHORT).show();
                            LogUtil.e("Download Finished" + getDownloadInfo().getDownloadFile().getRealPath());
                        }

                        @Override
                        public void onFailed() {
                            progressDialog.dismiss();
                            LogUtil.e("onFailed code=" + getDownloadInfo().getErrorCode());
                            Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                        }
                    })
                    //Set whether to repeatedly download the downloaded file,default false.
                    .forceReDownload(true)
                    //Set how many threads are used when downloading,default 3.
                    .threadNum(3)
                    .setId("123")
                    //Pump will connect server by this OKHttp request builder,so you can customize http request.
                    .setRequestBuilder(new Request.Builder())
                    .setRetry(3, 200)
                    .submit();
        });
        findViewById(R.id.add_download_list).setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                downloadList();
            } else {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                } else {
                    downloadList();
                }
            }
        });

        findViewById(R.id.jump_download_list).
                setOnClickListener(v -> {
                    boolean groupByTag = false;
                    DownloadListActivity.start(v.getContext(), groupByTag ? TAG : "");
                });
        findViewById(R.id.jump_remote_download_list).
                setOnClickListener(v -> RemoteDownloadListActivity.start(v.getContext()));
        findViewById(R.id.jump_webview_download).
                setOnClickListener(v -> WebViewDownloadActivity.start(v.getContext()));
    }

    private void downloadList() {
        Pump.newRequestToDownload(url, "/ababapump")
                .forceReDownload(true)
//                        .disableBreakPointDownload()
                .submit();
        Pump.newRequestToMovie(url4, "/ababapump/movie")
                .setDownloadTaskExecutor(DemoApplication.getInstance().musicDownloadDispatcher)
                .forceReDownload(true)
                .submit();
        Pump.newRequestToDownload(url4, "/ababapump")
                .setId(url4+"12")
                .setDownloadTaskExecutor(DemoApplication.getInstance().musicDownloadDispatcher)
                .forceReDownload(true)
                .submit();
        Pump.newRequestToPicture(url4, "/ababapump/picture")
                .setId(url4+"23")
                .setDownloadTaskExecutor(DemoApplication.getInstance().musicDownloadDispatcher)
                .forceReDownload(true)
                .submit();
        Pump.newRequestToMusic(url4, "/ababapump/music")
                .setId(url4+"45")
                .setDownloadTaskExecutor(DemoApplication.getInstance().musicDownloadDispatcher)
                .forceReDownload(true)
                .submit();
        Pump.newRequest(url2)
                .tag(TAG)
                .forceReDownload(true)
                .submit();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    downloadList();
                } else {
                    Toast.makeText(this, "权限申请失败", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public DownloadRequest.DownloadGenerator newRequestToDownload(String url, String directory) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return Pump.newRequest(url, Environment.DIRECTORY_DOWNLOADS + directory, MediaStore.Downloads.EXTERNAL_CONTENT_URI);
        } else {
            String dirPath = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS).getAbsolutePath()
                    + (directory == null ? "" : directory);
            return Pump.newRequest(url, dirPath);
        }
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
        //shutdown will stop all tasks and release some resource.
//        Pump.shutdown();
    }
}
