package com.huxq17.download.Utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

//    /**
//     * 合并文件
//     *
//     * @return {@code true} 合并成功，{@code false}合并失败
//     */
//    public static boolean mergeFiles(File[] sources, File dest) {
//        File[] sortedFiles = new File[sources.length];
//        //Sort temp files.
//        for (int i = 0; i < sources.length; i++) {
//            File partFile = sources[i];
//            String partFileName = partFile.getName();
//            int idIndex = partFileName.lastIndexOf("-") + 1;
//            int id = Integer.parseInt(partFileName.substring(idIndex));
//            if (id < sortedFiles.length) {
//                sortedFiles[id] = partFile;
//            } else {
//                return false;
//            }
//        }
//        FileOutputStream fos = null;
//        FileChannel foc = null;
//        SequenceInputStream sis = null;
//        try {
//            Vector<FileInputStream> streams = new Vector<>(sortedFiles.length);
//            for (int i = 0; i < sortedFiles.length; i++) {
//                File partFile = sortedFiles[i];
//                if (partFile.exists()) {
//                    if (i > 0) {
//                        streams.add(new FileInputStream(partFile));
//                    }
//                } else {
//                    for (FileInputStream fis : streams) {
//                        fis.close();
//                    }
//                    streams.clear();
//                    return false;
//                }
//            }
//            fos = new FileOutputStream(sortedFiles[0],true);
//            foc = fos.getChannel();
//            sis = new SequenceInputStream(streams.elements());
//            ReadableByteChannel fic = Channels.newChannel(sis);
//            ByteBuffer bf = ByteBuffer.allocate(8196);
//            while (fic.read(bf) != -1) {
//                bf.flip();
//                foc.write(bf);
//                bf.compact();
//            }
//            fic.close();
//            sis.close();
//            return renameTo(sortedFiles[0], dest);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            closeQuietly(sis);
//            closeQuietly(foc);
//            closeQuietly(fos);
//        }
//        return false;
//    }

    public static boolean mergeFiles(File[] sources, File dest) {
        File[] sortedFiles = new File[sources.length];
        for (int i = 0; i < sources.length; i++) {
            File partFile = sources[i];
            String partFileName = partFile.getName();
            int idIndex = partFileName.lastIndexOf("-") + 1;
            int id = Integer.parseInt(partFileName.substring(idIndex));
            if (id < sortedFiles.length) {
                sortedFiles[id] = partFile;
            } else {
                return false;
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
            }
            bufferedSink.flush();
            renameTo(sortedFiles[0], dest);
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(bufferedSink);
            closeQuietly(bufferedSource);
        }
        return false;
    }

    public static File getTempDir(String filePath) {
        File file = new File(filePath);
        File parentFile = file.getParentFile();
        return new File(parentFile, "." + file.getName() + ".temp" + File.separatorChar);
    }

    public static long getUsableSpace(File file) {
        if (file == null) return 0L;
        return getUsableSpaceBeforeO(file);
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
//            return getUsableSpaceBeforeO(file);
//        } else {
//
//        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static long getUsableSpaceAfterO(Context context, File file) {
        StorageManager sm = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            UUID uuid = sm.getUuidForPath(file);
            sm.getAllocatableBytes(uuid);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0L;
    }

    private static long getUsableSpaceBeforeO(File file) {
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

    /**
     * 为防止创建一个正在被删除的文件夹，所以在删除前先重命名该文件夹
     * 可以解决很多快速创建删除而产生的0字节大小文件问题
     *
     * @param file 文件对象
     * @return 是否成功
     */
    public static boolean deleteFile(File file) {
        File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
        file.renameTo(to);
        return to.delete();
    }

    /**
     * 重命名
     *
     * @param filePathName 原始文件路径
     * @param newPathName  新的文件路径
     * @return 是否成功
     */
    public static boolean rename(String filePathName, String newPathName) {
        if (TextUtils.isEmpty(filePathName)) return false;
        if (TextUtils.isEmpty(newPathName)) return false;

        delete(newPathName);

        File file = new File(filePathName);
        File newFile = new File(newPathName);
        if (!file.exists()) {
            return false;
        }
        File parentFile = newFile.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        return file.renameTo(newFile);
    }

    /**
     * 删除文件
     */
    public static boolean delete(String filePathName) {
        if (TextUtils.isEmpty(filePathName)) return false;
        File file = new File(filePathName);
        return deleteFile(file);
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
            return bytesToHexString(MD5.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            closeQuietly(fileInputStream);
        }
    }

    public static String getMD5ByStr(String src) {
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            byte[] buffer = src.getBytes();
            MD5.update(buffer);
            return bytesToHexString(MD5.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return "";
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }

    public static long parseContentLength(@Nullable String contentLength) {
        if (contentLength == null) return -1;

        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException ignored) {
        }

        return -1;
    }

    public static String guessFileName(
            String url,
            @Nullable String contentDisposition,
            @Nullable String mimeType) {
        String filename = null;
        String extension = null;

        // If we couldn't do anything with the hint, move toward the content disposition
        if (contentDisposition != null) {
            filename = parseContentDisposition(contentDisposition);
            if (filename != null) {
                int index = filename.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = filename.substring(index);
                }
            }
        }

        // If all the other http-related approaches failed, use the plain uri
        if (filename == null) {
            String decodedUrl = Uri.decode(url);
            if (decodedUrl != null) {
                int queryIndex = decodedUrl.indexOf('?');
                // If there is a query string strip it, same as desktop browsers
                if (queryIndex > 0) {
                    decodedUrl = decodedUrl.substring(0, queryIndex);
                }
                if (!decodedUrl.endsWith("/")) {
                    int index = decodedUrl.lastIndexOf('/') + 1;
                    if (index > 0) {
                        filename = decodedUrl.substring(index);
                    }
                }
            }
        }

        // Finally, if couldn't get filename from URI, get a generic filename
        if (filename == null) {
            filename = getMD5ByStr(url);
        }

        // Split filename between base and extension
        // Add an extension if filename does not have one
        int dotIndex = filename.indexOf('.');
        if (dotIndex >= 0) {
            if (mimeType != null) {
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                int lastDotIndex = filename.lastIndexOf('.');
                String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                        filename.substring(lastDotIndex + 1));
                if (typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null) {
                        extension = "." + extension;
                    }
                }
            }
        }
        if (extension == null) {
            if (mimeType != null) {
                int index = mimeType.indexOf(";");
                if (index >= 0) {
                    mimeType = mimeType.substring(0, index);
                }
                extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                if (extension != null) {
                    extension = "." + extension;
                }
            }
            if (extension == null) {
                if (mimeType != null && mimeType.toLowerCase(Locale.ROOT).startsWith("text/")) {
                    if (mimeType.equalsIgnoreCase("text/html")) {
                        extension = ".html";
                    } else {
                        extension = ".txt";
                    }
                }
            }
        }
        if (extension == null) {
            extension = filename.substring(dotIndex);
        }
        if (dotIndex >= 0) {
            filename = filename.substring(0, dotIndex);
        }
        return filename + extension;
    }

    /**
     * Parse the Content-Disposition HTTP Header. The format of the header
     * is defined here: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html
     * This header provides a filename for content that is going to be
     * downloaded to the file system. We only support the attachment type.
     * Note that RFC 2616 specifies the filename value must be double-quoted.
     * Unfortunately some servers do not quote the value so to maintain
     * consistent behaviour with other browsers, we allow unquoted values too.
     */
    static String parseContentDisposition(String contentDisposition) {
        try {
            Matcher m = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
            if (m.find()) {
                return m.group(2);
            }
        } catch (IllegalStateException ex) {
            // This function is defined as returning null when it can't parse the header
        }
        return null;
    }

    /**
     * Regex used to parse content-disposition headers
     */
    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("attachment;\\s*filename\\s*=\\s*(\"?)([^\"]*)\\1\\s*$",
                    Pattern.CASE_INSENSITIVE);
}