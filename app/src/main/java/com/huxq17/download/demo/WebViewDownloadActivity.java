package com.huxq17.download.demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.huxq17.download.Pump;
import com.huxq17.download.utils.LogUtil;

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
                printlnSession(request.getUrl().toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                printlnSession(url);
                return true;
            }
        };
        initProgressDialog();
        webView.setWebViewClient(webViewClient);
        webView.loadUrl("https://gofile.io/?c=dqPxFL");
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                progressDialog.setProgress(0);
                progressDialog.show();
                Pump.newRequest(url)
                        .listener(downloadListener)
                        .submit();
                printlnSession(url);
            }
        });
    }

    private com.huxq17.download.core.DownloadListener downloadListener = new com.huxq17.download.core.DownloadListener() {

        @Override
        public void onProgress(int progress) {
            progressDialog.setProgress(progress);
        }

        @Override
        public void onSuccess() {
            progressDialog.dismiss();

            Toast.makeText(WebViewDownloadActivity.this, "Download Finished", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            progressDialog.dismiss();
            LogUtil.e("onFailed code=" + getDownloadInfo().getErrorCode());
            Toast.makeText(WebViewDownloadActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
        }
    };

    private void printlnSession(String url) {
        CookieManager cookieManager = CookieManager.getInstance();
        String cookies = cookieManager.getCookie(url);
    }
}
