package com.huxq17.download.provider;

public class Provider {
    public static final String AUTHORITY = "com.huxq17.download.provider";

    public static final class DownloadBlock {
        public static final String TABLE_NAME = "download_block";
        public static final String URL = "url";
        public static final String THREAD_ID = "thread_id";
        public static final String COMPLETE_SIZE = "complete_size";
    }

    public static final class DownloadInfo {
        public static final String TABLE_NAME = "download_info";
        public static final String URL = "url";
        public static final String PATH = "path";
        public static final String THREAD_NUM = "thread_num";
        public static final String FILE_LENGTH = "file_length";
        public static final String FINISHED = "finished";
    }
}
