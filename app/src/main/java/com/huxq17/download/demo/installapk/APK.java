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
        private Uri uri;
        private boolean forceInstall;

        public APKBuilder(Context context) {
            this.context = context;
        }

        public APKBuilder from(String apkPath) {
            this.apkPath = apkPath;
            return this;
        }

        public APKBuilder from(Uri apkUri) {
            this.uri = apkUri;
            return this;
        }

        public void forceInstall() {
            this.forceInstall = true;
            install();
        }

        public void install() {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri contentUri = this.uri;
            if (contentUri == null) {
                File file = new File(apkPath);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                     contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider-installApk", file);
                } else {
                    contentUri = Uri.fromFile(file);
                }
            }
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }
}
