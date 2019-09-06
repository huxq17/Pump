package com.huxq17.download.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.UUID;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class Util {
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

    public static boolean deleteDir(File dirFile) {
        if (!dirFile.exists()) {
            return false;
        }
        if (dirFile.isFile()) {
            return dirFile.delete();
        } else {
            File[] children = dirFile.listFiles();
            if (children != null) {
                for (File file : children) {
                    deleteDir(file);
                }
            }
        }
        return dirFile.delete();
    }

    public static boolean renameTo(File source, File dest) {
        if (dest.exists()) {
            if (dest.delete()) {
                return source.renameTo(dest);
            }
            return false;
        }
        return source.renameTo(dest);
    }

    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getCachePathByUrl(Context context, String url) {
        String apkName = getFileNameByUrl(url);
        return getCachePath(context) + "/" + apkName;
    }

    public static String getCachePath(Context context) {
        File externalCacheDir = context.getExternalCacheDir();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            if (externalCacheDir != null) {
                return externalCacheDir.getAbsolutePath();
            } else {
                if (hasStoragePermission(context)) {
                    File cacheFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getPackageName() + "/cache/");
                    if (!cacheFile.exists()) {
                        cacheFile.mkdirs();
                    }
                    return cacheFile.getAbsolutePath();
                } else {
                    return context.getCacheDir().getAbsolutePath();
                }
            }
        } else {
            return context.getCacheDir().getAbsolutePath();
        }
    }

    public static String getFileNameByUrl(String url) {
        int index = url.indexOf("?");
        String fileName = null;
        try {
            if (index != -1) {
                fileName = url.substring(url.lastIndexOf("/") + 1, url.indexOf("?"));
            } else {
                fileName = url.substring(url.lastIndexOf("/") + 1);
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

        if (TextUtils.isEmpty(fileName)) {
            fileName = UUID.randomUUID().toString();
        }
        return fileName;
    }

//    public static void mergeFiles(File[] sources, File dest) {
//        if (sources == null || sources.length <= 0) {
//            return;
//        }
//        if (sources.length == 1) {
//            renameTo(sources[0], dest);
//            return;
//        }
//        File[] sortedFiles = new File[sources.length];
//        for (int i = 0; i < sources.length; i++) {
//            File partFile = sources[i];
//            String partFileName = partFile.getName();
//            int idIndex = partFileName.lastIndexOf("-") + 1;
//            int id = Integer.parseInt(partFileName.substring(idIndex));
//            sortedFiles[id] = partFile;
//        }
//        FileChannel destFileChannel = null;
//        FileChannel inputChannel = null;
//        try {
//            destFileChannel = new FileOutputStream(sortedFiles[0], true).getChannel();
//            for (int i = 1; i < sources.length; i++) {
//                inputChannel = new FileInputStream(sources[i]).getChannel();
//                destFileChannel.transferFrom(inputChannel, destFileChannel.size(), inputChannel.size());
//                inputChannel.close();
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            closeQuietly(destFileChannel);
//            closeQuietly(inputChannel);
//        }
//        renameTo(sortedFiles[0], dest);
//    }

    public static void mergeFiles(File[] sources, File dest) {
        File[] sortedFiles = new File[sources.length];
        for (int i = 0; i < sources.length; i++) {
            File partFile = sources[i];
            String partFileName = partFile.getName();
            int idIndex = partFileName.lastIndexOf("-") + 1;
            int id = Integer.parseInt(partFileName.substring(idIndex));
            if (id < sortedFiles.length) {
                sortedFiles[id] = partFile;
            } else {
                return;
            }
        }
        BufferedSink bufferedSink = null;
        BufferedSource bufferedSource = null;
        try {
            byte[] buffer = new byte[8092];
            int len;
            bufferedSink = Okio.buffer(Okio.appendingSink(sortedFiles[0]));
            for (int i = 1; i < sortedFiles.length; i++) {
                File file = sortedFiles[i];
                bufferedSource = Okio.buffer(Okio.source(file));
                while ((len = bufferedSource.read(buffer)) != -1) {
                    bufferedSink.write(buffer, 0, len);
                }
                closeQuietly(bufferedSource);
                file.delete();
            }
            bufferedSink.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(bufferedSink);
            closeQuietly(bufferedSource);
        }
        renameTo(sortedFiles[0], dest);
    }

    public static File getTempDir(String filePath) {
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        return new File(parentFile, "." + file.getName() + ".temp" + File.separatorChar);
    }

    public static long getUsableSpace(File file) {
        if (file == null) return 0L;
        if (file.isDirectory()) {
            return file.getUsableSpace();
        } else {
            File parentFile = file.getParentFile();
            if (parentFile != null) {
                return parentFile.getUsableSpace();
            } else {
                return 0L;
            }
        }
    }

    public static void copyFile(File sourceFile, File destFile) {
        BufferedSource bufferedSource = null;
        BufferedSink bufferedSink = null;
        try {
            bufferedSource = Okio.buffer(Okio.source(sourceFile));
            bufferedSink = Okio.buffer(Okio.sink(destFile));
            bufferedSink.writeAll(bufferedSource);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(bufferedSink);
            closeQuietly(bufferedSource);
        }

    }

    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return getMd5StrFromBytes(MD5.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            closeQuietly(fileInputStream);
        }
    }

    /**
     * MD5sum for string
     */
    public static String getMd5StrFromBytes(byte[] md5bytes) {
        if (md5bytes == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < md5bytes.length; i++) {
            sb.append(String.format("%02x", md5bytes[i]));
        }
        return sb.toString();
    }

}