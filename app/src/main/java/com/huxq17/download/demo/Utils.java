package com.huxq17.download.demo;

import androidx.annotation.NonNull;
import android.util.Base64;

import com.huxq17.download.callback.Func;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;

public class Utils {
    private static OkHttpClient OK_HTTP_CLIENT;

    public static OkHttpClient getIgnoreCertificateOkHttpClient() {
        if (OK_HTTP_CLIENT == null) {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
//                .cache(cache)
                    .followRedirects(true)
                    .retryOnConnectionFailure(true)
                    .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .connectTimeout(15, TimeUnit.SECONDS);

            try {
                // Create a trust manager that does not validate certificate chains
                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                    throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                final SSLContext sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Create an ssl socket factory with our all-trusting manager
                final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
                builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
                builder.hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            OK_HTTP_CLIENT = builder.build();
        }
        return OK_HTTP_CLIENT;
    }

    public static String getDataSize(long size) {
        if (size < 0) {
            size = 0;
        }
        DecimalFormat format = new DecimalFormat("####.00");
        if (size < 1024) {
            return size + "bytes";
        } else if (size < 1024 * 1024) {
            float kbSize = size / 1024f;
            return format.format(kbSize) + "KB";
        } else if (size < 1024 * 1024 * 1024) {
            float mbSize = size / 1024f / 1024f;
            return format.format(mbSize) + "MB";
        } else {
            float gbSize = size / 1024f / 1024f / 1024f;
            return format.format(gbSize) + "GB";
        }
    }

    public static String getMD5ByBase64(File file) {
        FileInputStream fileInputStream = null;

        String var3;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];

            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return Base64.encodeToString(MD5.digest(), Base64.NO_WRAP);
        } catch (Exception var9) {
            var9.printStackTrace();
            var3 = "";
        } finally {
            Util.closeQuietly(fileInputStream);
        }

        return var3;
    }

    public static List<File> listFiles(File startDir, String exclude) {
        final List<File> files = new ArrayList<>();
        listFilesWithCallback(startDir, exclude, new Func<File>() {
            @Override
            public void call(@NonNull File result) {
                files.add(result);
            }
        });
        return files;
    }

    public static void listFilesWithCallback(File startDir, String exclude, Func<File> callback) {
        final LinkedList<File> dirs = new LinkedList<>();
        dirs.add(startDir);
        while (!dirs.isEmpty()) {
            final File dir = dirs.removeFirst();
            if (dir.getName().equals(exclude)) continue;
            final File[] children = dir.listFiles();
            for (File child : children) {
                if (child.isDirectory()) {
                    dirs.add(child);
                } else if (child.isFile()) {
                    callback.call(child);
                }
            }
        }
    }

    public static boolean unzipFile(String zipFile, String toDir, String fileName) {
        boolean result;
        byte[] buffer = new byte[8192];
        InputStream in = null;
        ZipInputStream zipIn = null;
        FileOutputStream out = null;
        try {
            File file = new File(zipFile);
            in = new FileInputStream(file);
            zipIn = new ZipInputStream(in);
            ZipEntry entry;
            while (null != (entry = zipIn.getNextEntry())) {
                String zipName = entry.getName();
                if (zipName.startsWith(fileName)) {
                    String relName = toDir + zipName;
                    File unzipFile = new File(toDir);
                    if (unzipFile.isDirectory()) {
                        createDirWithFile(relName);
                        unzipFile = new File(relName);
                    }
                    out = new FileOutputStream(unzipFile);
                    int bytes;

                    while ((bytes = zipIn.read(buffer, 0, 8192)) != -1) {
                        out.write(buffer, 0, bytes);
                    }
                }
            }
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            Util.closeQuietly(in);
            Util.closeQuietly(zipIn);
            Util.closeQuietly(out);
        }
        return result;
    }

    /**
     * 创建目录，整个路径上的目录都会创建
     *
     * @param path 路径
     * @return 文件
     */
    public static File createDirWithFile(String path) {
        File file = new File(path);
        if (!path.endsWith("/")) {
            file = file.getParentFile();
        }
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }
}
