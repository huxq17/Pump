package com.huxq17.download.provider;

import android.content.Context;
import android.net.Uri;

public class Provider {
    public static final String AUTHORITY_URI = "content://%s.huxq17.download-provider";
    public static Uri CONTENT_URI;

    public static Uri getContentUri(Context context) {
        if (CONTENT_URI == null) {
            CONTENT_URI = Uri.parse(String.format(AUTHORITY_URI, context.getPackageName()));
        }
        return CONTENT_URI;
    }

    public static final class DownloadTable {
        public static final String TABLE_NAME = "download_info";
        public static final String URL = "url";
        public static final String PATH = "path";
        public static final String THREAD_NUM = "thread_num";
        public static final String FILE_LENGTH = "file_length";
        public static final String FINISHED = "finished";
        public static final String CREATE_TIME = "create_time";
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

        public CacheBean(String url, String lastModified, String eTag) {
            this.lastModified = lastModified;
            this.eTag = eTag;
            this.url = url;
        }

    }
}
