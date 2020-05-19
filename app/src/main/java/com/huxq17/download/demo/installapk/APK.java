package com.huxq17.download.demo.installapk;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import androidx.core.content.FileProvider;

import java.io.File;

public class APK {
    public static APKBuilder with(Context context) {
        return new APKBuilder(context);
    }

    public static class APKBuilder {
        private Context context;
        private String apkPath;
        private String uri;
        private boolean forceInstall;

        public APKBuilder(Context context) {
            this.context = context;
        }

        public APKBuilder from(String apkPath) {
            this.apkPath = apkPath;
            return this;
        }

        public APKBuilder fromUri(String apkUri) {
            this.uri = apkUri;
            return this;
        }

        public void forceInstall() {
            this.forceInstall = true;
            install();
        }

        public void install() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File file = new File(apkPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider-installApk", file);
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
//            RequestInstallPermissionActivity.start(context, apkPath, uri, forceInstall);
        }
    }
}
