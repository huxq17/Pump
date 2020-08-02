package com.huxq17.download.demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.huxq17.download.Pump;
import com.huxq17.download.utils.LogUtil;

import java.io.File;

import okhttp3.Request;

public class WebViewDownloadActivity extends AppCompatActivity {
    private ProgressDialog progressDialog;

    public static void start(Context context) {
        Intent intent = new Intent(context, WebViewDownloadActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }

    private void initProgressDialog() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Downloading");
//        progressDialog.setMessage("Downloading now...");
        progressDialog.setProgress(0);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.act_webview);
        WebView webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        WebViewClient webViewClient = new WebViewClient() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        };
        initProgressDialog();
        webView.setWebViewClient(webViewClient);
        webView.loadUrl("http://www.mtv-ktv.net/mv/mtv15/ktv143092.htm");
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                progressDialog.setProgress(0);
                progressDialog.show();
                File file = Pump.getFileIfSucceed(url);
                if (file == null) {
                    Pump.newRequest(url)
                            .setRequestBuilder(new Request.Builder()
                                    .addHeader("referer", "http://www.mtv-ktv.net/mv/mtv15/ktv143092.htm"))
                            .setId(WebViewDownloadActivity.class.getCanonicalName())
                            .listener(downloadListener)
                            .threadNum(1)
                            .submit();
                } else {
                    playVideo(file);
                }
            }
        });
    }

    private void playVideo(File videoFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName()+".fileProvider-installApk", videoFile);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType("video/*") ;
        } else {
            intent.setType("video/*") ;
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(videoFile));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, "播放"));
    }

    private com.huxq17.download.core.DownloadListener downloadListener = new com.huxq17.download.core.DownloadListener() {

        @Override
        public void onProgress(int progress) {
            progressDialog.setProgress(progress);
        }

        @Override
        public void onSuccess() {
            progressDialog.dismiss();
            playVideo(new File(getDownloadInfo().getFilePath()));
            Toast.makeText(WebViewDownloadActivity.this, "Download Finished", Toast.LENGTH_SHORT).show();

        }

        @Override
        public void onFailed() {
            progressDialog.dismiss();
            LogUtil.e("onFailed code=" + getDownloadInfo().getErrorCode());
            Toast.makeText(WebViewDownloadActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        downloadListener.disable();
    }
}
