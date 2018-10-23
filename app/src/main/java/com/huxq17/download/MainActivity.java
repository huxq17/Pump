package com.huxq17.download;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.huxq17.download.listener.StatusObserver;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private String url = "http://www.clcxx.com/app/clcxx.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File file3 = new File(getExternalCacheDir().getAbsolutePath(), "download.apk");
        Pump.from(url).into(file3.getAbsolutePath()).subscribe(new StatusObserver() {
            @Override
            public void onProgressUpdate(int progress) {

            }

            @Override
            public void onError(int errorCode) {

            }
        });
    }
}
