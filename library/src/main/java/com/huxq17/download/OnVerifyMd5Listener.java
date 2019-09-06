package com.huxq17.download;

import java.io.File;

public interface OnVerifyMd5Listener {
    /**
     * Verify md5
     * @param md5 Get from http response header(Content-MD5)
     * @param file download file
     * @return
     */
    boolean onVerifyMd5(String md5, File file);
}
