package com.huxq17.download;

import java.io.File;

public class DownloadBatch {
    public String url;
    public int threadId;
    public long downloadedSize;

    public long startPos;
    public long endPos;
    public File tempFile;

    public void calcuStartPos(long fileLength, int threadNum) {
        if (startPos == 0) {
            startPos = threadId * fileLength / threadNum;
        }
    }

    public void calcuEndPos(long fileLength, int threadNum) {
        if (endPos == 0) {
            if (threadNum == threadId + 1) {
                endPos = fileLength - 1;
            } else {
                endPos = (threadId + 1) * fileLength / threadNum - 1;
            }
        }
    }

    public boolean hasFinished() {
        return endPos - startPos == downloadedSize;
    }
}
