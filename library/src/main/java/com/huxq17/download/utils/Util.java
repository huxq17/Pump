package com.huxq17.download.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.core.DownloadDetailsInfo;
import com.huxq17.download.core.PumpFile;
import com.huxq17.download.core.task.DownloadTask;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Response;

public class Util {
    public static final String DOWNLOAD_PART = "DOWNLOAD_PART-";
    public static final String PUMP_CACHE_DIRECTORY_SUB = "pump_cache" + File.separatorChar;
    public static final String BIN = "bin";
    public static final String TRANSFER_ENCODING_CHUNKED = "chunked";
    public static final int CONTENT_LENGTH_NOT_FOUND = -1;

    private Util() {
    }

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

    public static boolean hasStoragePermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static String getPumpCachePath(Context context) {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File cacheFile = context.getExternalFilesDir(PUMP_CACHE_DIRECTORY_SUB);
            if (cacheFile != null) {
                if (cacheFile.exists()) {
                    return cacheFile.getAbsolutePath();
                } else {
                    if (cacheFile.mkdirs()) {
                        return cacheFile.getAbsolutePath();
                    }
                }
            }
        }
        File cacheFile = new File(context.getFilesDir(), PUMP_CACHE_DIRECTORY_SUB);
        if (!cacheFile.exists()) {
            cacheFile.mkdirs();
        }
        return cacheFile.getAbsolutePath();
    }

    public static File getTempDir(String url) {
        return new File(getPumpCachePath(DownloadProvider.context), url.replace(File.separator, "_") + File.separatorChar);
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
                //Create parent directory if not exists.
                if (!parentFile.exists()) {
                    return parentFile.mkdirs() ? parentFile.getUsableSpace() : 0L;
                }
                return parentFile.getUsableSpace();
            }
            return 0L;
        }
    }

    public static long parseContentLength(@Nullable String contentLength) {
        if (contentLength == null) return CONTENT_LENGTH_NOT_FOUND;

        try {
            return Long.parseLong(contentLength);
        } catch (NumberFormatException ignored) {
        }

        return CONTENT_LENGTH_NOT_FOUND;
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
            filename = MD5Util.getMD5ByStr(url);
        }

        // Split filename between base and extension
        // Add an extension if filename does not have one
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0) {
            if (mimeType != null) {
                // Compare the last segment of the extension against the mime type.
                // If there's a mismatch, discard the entire extension.
                String urlExt = filename.substring(dotIndex + 1);
                String typeFromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(urlExt);
                if (typeFromExt != null && !typeFromExt.equalsIgnoreCase(mimeType)) {
                    extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
                    if (extension != null && !extension.equalsIgnoreCase(BIN)) {
                        extension = "." + extension;
                    } else {
                        extension = "." + urlExt;
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

        if (dotIndex >= 0) {
            if (extension == null) {
                extension = filename.substring(dotIndex);
            }
            filename = filename.substring(0, dotIndex);
        }
        if (extension != null) {
            filename = filename + extension;
        }
        return filename;
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