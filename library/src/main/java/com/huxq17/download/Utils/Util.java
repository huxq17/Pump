package com.huxq17.download.Utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
            File[] subFiles = dirFile.listFiles();
            if (subFiles != null) {
                for (File file : subFiles) {
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

    public static String getCachePathByUrl(Context context, String url) {
        String apkName = getFileNameByUrl(url);
        return getCachePath(context) + "/" + apkName;
    }


    public static String getCachePath(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return context.getExternalCacheDir().getAbsolutePath();
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
            sortedFiles[id] = partFile;
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
        File tempDir = new File(parentFile, "." + file.getName() + ".temp" + File.separatorChar);
        return tempDir;
    }
}
