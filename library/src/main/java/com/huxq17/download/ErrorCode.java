package com.huxq17.download;

public class ErrorCode {
    /**
     * Network unavailable
     */
    public static final int NETWORK_UNAVAILABLE = 2001;
    /**
     * Website return 404.
     */
    public static final int FILE_NOT_FOUND = 1002;
    /**
     * Server unknown error.
     */
    public static final int UNKNOWN_SERVER_ERROR = 1003;
    /**
     * Breakpoint download need request file's length first,but not found in http response head,connect server to resolve it.
     */
    public static final int CONTENT_LENGTH_NOT_FOUND = 1004;
}
