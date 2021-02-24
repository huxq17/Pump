package com.huxq17.download;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huxq17.download.config.DownloadConfigService;
import com.huxq17.download.core.service.IDownloadConfigService;
import com.huxq17.download.core.DownloadManager;
import com.huxq17.download.db.DBService;
import com.huxq17.download.core.service.IDownloadManager;
import com.huxq17.download.core.service.IMessageCenter;
import com.huxq17.download.core.MessageCenter;
import com.huxq17.download.utils.OKHttpUtil;
import com.huxq17.download.utils.ReflectUtil;

public class DownloadProvider extends ContentProvider {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    public static final String AUTHORITY_URI = "content://%s.huxq17.download-provider";
    public static Uri CONTENT_URI;

    public static Uri getContentUri(Context context) {
        if (CONTENT_URI == null) {
            CONTENT_URI = Uri.parse(String.format(AUTHORITY_URI, context.getPackageName()));
        }
        return CONTENT_URI;
    }

    @Override
    public boolean onCreate() {

        context = getContext();
        DBService.init(context);
        DownloadManager downloadManager = ReflectUtil.newInstance(DownloadManager.class);
        downloadManager.start(context);
        PumpFactory.addService(IDownloadManager.class, downloadManager);
        MessageCenter messageCenter = ReflectUtil.newInstance(MessageCenter.class);
        messageCenter.start(context);
        PumpFactory.addService(IMessageCenter.class, messageCenter);
        IDownloadConfigService downloadConfig = ReflectUtil.newInstance(DownloadConfigService.class);
        PumpFactory.addService(IDownloadConfigService.class, downloadConfig);
        OKHttpUtil.init(context);
        return true;
    }

    public static final class DownloadTable {
        public static final String TABLE_NAME = "download_info";
        public static final String ID = "id";
        public static final String URL = "url";
        public static final String PATH = "path";
        public static final String THREAD_NUM = "thread_num";
        public static final String FILE_LENGTH = "file_length";
        public static final String FINISHED = "finished";
        public static final String CREATE_TIME = "create_time";
        public static final String TAG = "tag";
        public static final String SCHEMA_URI = "schema_uri";
        public static final String MIME_TYPE = "mime_type";
    }

    public static final class CacheTable {
        public static final String TABLE_NAME = "download_cache";
        public static final String URL = "url";
        public static final String LAST_MODIFIED = "Last_modified";
        public static final String ETAG = "eTag";
    }

    public static final class CacheBean {
        public String lastModified;
        public String eTag;
        public String url;

        public String getIfRangeField() {
            return TextUtils.isEmpty(eTag) ? lastModified : eTag;
        }

        public CacheBean(String url, String lastModified, String eTag) {
            this.lastModified = lastModified;
            this.eTag = eTag;
            this.url = url;
        }
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri,
                        @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        throw new SQLException("Not support to query.");
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new SQLException("Not support to delete.");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new SQLException("Not support to update.");
    }
}
