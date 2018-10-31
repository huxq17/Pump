package com.huxq17.download;

public class ErrorCode {
    /**
     * download file already exists,and {@link DownloadConfig#forceReDownload} is false.
     */
    public static final int FILE_ALREADY_EXISTS = 1000;
    /**
     * Network unavailable
     */
    public static final int NETWORK_UNAVAILABLE = 1001;
    /**
     * Breakpoint download need request file's length first,but not found in http response head,connect server to resolve it.
     */
    public static final int CONTENT_LENGTH_NOT_FOUND = 1002;
}
