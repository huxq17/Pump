package com.huxq17.download;

public enum ErrorCode {

    /**
     * Network unavailable
     */
    ERROR_NETWORK_UNAVAILABLE,
    /**
     * create download file failed.
     */
    ERROR_CREATE_FILE_FAILED,
    /**
     * Website return 404.
     */
    ERROR_FILE_NOT_FOUND,
    /**
     * Server unknown error.
     */
    ERROR_UNKNOWN_SERVER_ERROR,
    /**
     * Usable space is not enough.
     */
    ERROR_USABLE_SPACE_NOT_ENOUGH,
    /**
     * Merge file failed.
     */
    ERROR_MERGE_FILE_FAILED,
    /**
     * Network unavailable
     */
    ERROR_CONTENT_LENGTH_NOT_FOUND,
    /**
     * Download file failed.
     */
    ERROR_DOWNLOAD_FAILED,
    /**
     * file's end is less than it's start.
     */
    ERROR_FILE_OUT_LIMIT;
}
