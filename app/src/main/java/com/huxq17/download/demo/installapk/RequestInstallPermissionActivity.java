package com.huxq17.download.demo.installapk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class RequestInstallPermissionActivity extends AppCompatActivity {
    private String apkPath;
    private String apkUri;
    private boolean forceInstall;
    private String authority;
    private static final int INSTALL_PACKAGES_REQUEST_CODE = 101;
    private static final int GET_UNKNOWN_APP_SOURCES = 102;
    private static final int INSTALL_APP_REQUEST_CODE = 103;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        apkPath = getIntent().getStringExtra("path");
        apkUri = getIntent().getStringExtra("uri");
        forceInstall = getIntent().getBooleanExtra("forceInstall", false);
        if (apkUri == null && TextUtils.isEmpty(apkPath)) {
            throw new RuntimeException("apkUri is null && apkPath is Empty");
        }
        authority = getPackageName() + ".fileProvider-installApk";
        checkInstallPermission(this);
    }

    public void checkInstallPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= 26) {
            boolean b = activity.getPackageManager().canRequestPackageInstalls();
            if (b) {
                installApk();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.REQUEST_INSTALL_PACKAGES}, INSTALL_PACKAGES_REQUEST_CODE);
            }
        } else {
            installApk();
        }
    }

    private void installApk() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        int currentApiVersion = android.os.Build.VERSION.SDK_INT;
        Uri u;
        if (currentApiVersion < 24) {
            if (!TextUtils.isEmpty(apkUri)) {
                u = Uri.parse(apkUri);
            } else {
                u = Uri.fromFile(new File(apkPath));
            }
        } else {
            File file = null;
            if (!TextUtils.isEmpty(apkUri)) {
                try {
                    file = new File(new URI(apkUri));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    finish();
                }
            } else {
                file = new File(apkPath);
            }
            if (file == null) return;
            u = FileProvider.getUriForFile(this, authority, file);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        intent.setDataAndType(u, "application/vnd.android.package-archive");
        startActivityForResult(intent, INSTALL_APP_REQUEST_CODE);
//        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case INSTALL_PACKAGES_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    installApk();
                } else {
                    Uri packageURI = Uri.parse("package:" + getPackageName());
                    Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
                    startActivityForResult(intent, GET_UNKNOWN_APP_SOURCES);
                }
                break;

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case INSTALL_APP_REQUEST_CODE:
                finish();
                break;
            case GET_UNKNOWN_APP_SOURCES:
                if (resultCode == RESULT_OK) {
                    installApk();
                } else {
                    if (forceInstall) {
                        checkInstallPermission(this);
                    } else {
                        finish();
                    }
                }
                break;
        }
    }

    public static void start(Context context, String apkPath, String apkUri, boolean forceInstall) {
        Intent intent = new Intent(context, RequestInstallPermissionActivity.class);
        intent.putExtra("path", apkPath);
        intent.putExtra("uri", apkUri);
        intent.putExtra("forceInstall", forceInstall);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
