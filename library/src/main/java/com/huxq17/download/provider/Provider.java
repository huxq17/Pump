package com.huxq17.download.provider;

import android.net.Uri;

public class Provider {
    public static final String AUTHORITY = "com.huxq17.download.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    public static final class DownloadInfo {
        public static final String TABLE_NAME = "download_info";
        public static final String URL = "url";
        public static final String PATH = "path";
        public static final String THREAD_NUM = "thread_num";
        public static final String FILE_LENGTH = "file_length";
        public static final String FINISHED = "finished";
        public static final String CREATE_TIME = "create_time";
    }
}
