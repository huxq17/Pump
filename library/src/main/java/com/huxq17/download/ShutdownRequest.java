package com.huxq17.download;

/**
 * Use for shutdown #{@link com.huxq17.download.DownloadService}
 */
public class ShutdownRequest extends DownloadRequest {

    protected ShutdownRequest() {
        super(null, null);
    }
}
