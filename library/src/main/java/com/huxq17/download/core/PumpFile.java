package com.huxq17.download.core;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import com.huxq17.download.DownloadProvider;
import com.huxq17.download.utils.FileUtil;
import com.huxq17.download.utils.Util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

import static com.huxq17.download.utils.Util.closeQuietly;

public class PumpFile {
    private File file;
    private String filePath;
    private String pathAboveQ;
    private final ContentResolver contentResolver;
    private Uri contentUri;
    private final Uri schemaUri;

    public PumpFile(String filePath, Uri schemaUri) {
        this.file = new File(filePath);
        this.filePath = filePath;
        this.schemaUri = schemaUri;
        contentResolver = DownloadProvider.context.getContentResolver();
    }

    public void setPath(String filePath) {
        this.file = new File(filePath);
        this.filePath = filePath;
    }

    public File getFile() {
        if (getSchemaUri() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryFileContentUri();
            if (pathAboveQ == null) {
                return null;
            } else {
                return new File(pathAboveQ);
            }
        } else {
            return file;
        }
    }

    public boolean isDirectory() {
        return filePath.endsWith(File.separator);
    }

    public String getPath() {
        return filePath;
    }

    public String getName() {
        return isDirectory() ? "" : file.getName();
    }

    public String getParent() {
        return file.getParent();
    }

    public Uri getSchemaUri() {
        return schemaUri;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public String getRealPath() {
        if (shouldUseUri()) {
            queryFileContentUri();
            return pathAboveQ;
        } else {
            return filePath;
        }
    }

    public long length() {
        long length = 0;
        if (shouldUseUri()) {
            if (contentUri != null) {
                Cursor cursor = contentResolver.query(getQueryUri(contentUri),
                        new String[]{MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA},
                        buildQueryBundle(null, null), null);
                if (cursor != null && cursor.moveToFirst()) {
                    length = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
                    pathAboveQ = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                }
                Util.closeQuietly(cursor);
            } else {
                String queryPathKey = MediaStore.MediaColumns.RELATIVE_PATH;
                String selection = queryPathKey + "=? and " + MediaStore.MediaColumns.DISPLAY_NAME + "=?";
                String relativePath = file.getParent();
                if (relativePath == null) {
                    throw new IllegalArgumentException("relativePath is null.");
                }
                if (!relativePath.endsWith(File.separator)) {
                    relativePath = relativePath + File.separator;
                }
                Cursor cursor = contentResolver.query(getQueryUri(schemaUri),
                        new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA},
                        buildQueryBundle(selection, new String[]{relativePath, file.getName()}),
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    length = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE));
                    pathAboveQ = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                    contentUri = Uri.withAppendedPath(schemaUri, "" + cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID)));
                }
                Util.closeQuietly(cursor);
            }
        } else {
            length = file.length();
        }
        if (length == 0 && pathAboveQ != null) {
            length = new File(pathAboveQ).length();
        }
        return length;
    }

    public boolean createNewFile() {
        return createNewFile(false);
    }

    public boolean createNewFile(boolean isPending) {
        if (shouldUseUri()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Files.FileColumns.DISPLAY_NAME, file.getName());
            contentValues.put(MediaStore.Files.FileColumns.RELATIVE_PATH, file.getParent());
            if (isPending) {
                contentValues.put(MediaStore.Files.FileColumns.IS_PENDING, 1);
            }
            try {
                contentUri = contentResolver.insert(schemaUri, contentValues);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            //correct file name.
            if (contentUri != null) {
                Cursor cursor = contentResolver.query(getQueryUri(contentUri),
                        new String[]{MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA},
                        buildQueryBundle(null, null), null);
                if (cursor != null && cursor.moveToFirst()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH));
                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
                    pathAboveQ = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                    if (!name.equals(file.getName())) {
                        file = new File(path, name);
                        filePath = file.getPath();
                    }
                }
                Util.closeQuietly(cursor);
            }
            return contentUri != null;
        } else {
            return FileUtil.createNewFile(file);
        }
    }

    public boolean exists() {
        if (shouldUseUri()) {
            contentUri = null;
            queryFileContentUri();
            return contentUri != null;
        } else {
            return file.exists();
        }
    }

    public boolean delete() {
        if (shouldUseUri()) {
            int result;
            String queryPathKey = MediaStore.MediaColumns.RELATIVE_PATH;
            String selection = queryPathKey + "=? and " + MediaStore.MediaColumns.DISPLAY_NAME + "=?";
            String relativePath = file.getParent();
            if (relativePath == null) {
                throw new IllegalArgumentException("relativePath is null.");
            }
            if (!relativePath.endsWith(File.separator)) {
                relativePath = relativePath + File.separator;
            }
            String[] selectionArgs = new String[]{relativePath, file.getName()};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                result = DownloadProvider.context.getContentResolver().delete(schemaUri, buildQueryBundle(selection, selectionArgs));
            } else {
                result = DownloadProvider.context.getContentResolver().delete(getQueryUri(schemaUri), selection, selectionArgs);
            }
            contentUri = null;
            return result == 1;
        }
        return FileUtil.deleteFile(file);
    }

    public boolean mergeFiles(File[] sources) {
        File[] sortedFiles = new File[sources.length];
        for (File partFile : sources) {
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
        try {
            OutputStream outputStream = getOutputStream();
            if (outputStream == null) return false;
            bufferedSink = Okio.buffer(Okio.sink(outputStream));
            for (File source : sortedFiles) {
                if (!appendFile(source, bufferedSink)) {
                    return false;
                }
            }
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(bufferedSink);
            if (contentUri != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    contentResolver.update(contentUri, contentValues, buildQueryBundle(null, null));
                } else {
                    contentResolver.update(getQueryUri(contentUri), contentValues, null, null);
                }
            }
        }
        return false;
    }

    public boolean appendFile(File sourceFile, BufferedSink bufferedSink) {
        if (bufferedSink == null) {
            return false;
        }
        BufferedSource bufferedSource = null;
        try {
            bufferedSource = Okio.buffer(Okio.source(sourceFile));

            byte[] buffer = new byte[8092];
            int len;
            while ((len = bufferedSource.read(buffer)) != -1) {
                bufferedSink.write(buffer, 0, len);
            }
            bufferedSink.flush();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(bufferedSource);
        }
        return false;
    }

    public OutputStream getOutputStream() throws FileNotFoundException {
        if (!exists() && !createNewFile()) {
            return null;
        }
        if (shouldUseUri()) {
            if (contentUri == null) {
                return null;
            }
            return contentResolver.openOutputStream(contentUri);
        } else {
            return new FileOutputStream(file);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void queryFileContentUri() {
        Integer fileMediaStoreId = null;
        String queryPathKey = MediaStore.MediaColumns.RELATIVE_PATH;
        String selection = queryPathKey + "=? and " + MediaStore.MediaColumns.DISPLAY_NAME + "=?";
        String relativePath = file.getParent();
        if (relativePath == null) {
            throw new IllegalArgumentException("relativePath is null.");
        }
        if (!relativePath.endsWith(File.separator)) {
            relativePath = relativePath + File.separator;
        }
        String[] selectionArgs = new String[]{relativePath, file.getName()};
        Cursor cursor = contentResolver.query(getQueryUri(schemaUri), new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA, queryPathKey, MediaStore.MediaColumns.DISPLAY_NAME},
                buildQueryBundle(selection, selectionArgs), null);
        if (cursor != null && cursor.moveToFirst()) {
            fileMediaStoreId = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            pathAboveQ = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DATA));
        }
        Util.closeQuietly(cursor);
        if (fileMediaStoreId != null) {
            contentUri = Uri.withAppendedPath(schemaUri, fileMediaStoreId.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getQueryUri(Uri uri) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R ? MediaStore.setIncludePending(uri) : uri;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private Bundle buildQueryBundle(String selection, String[] selectionArgs) {
        Bundle queryBundle = new Bundle();
        queryBundle.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection);
        queryBundle.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            queryBundle.putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE);
        }
        return queryBundle;
    }

    public boolean shouldUseUri() {
        return getSchemaUri() != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q;
    }
}
