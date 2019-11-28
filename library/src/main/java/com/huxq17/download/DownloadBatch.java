package com.huxq17.download;

import java.io.File;
import java.io.IOException;

public class DownloadBatch {
    public String url;
    public int threadId;
    public long downloadedSize;

    public long startPos;
    public long endPos;
    public File tempFile;
    public static final String DOWNLOAD_PART = "DOWNLOAD_PART-";

    public long calculateCompletedPartSize(File tempDir) {
        tempFile = new File(tempDir, DOWNLOAD_PART + threadId);
        if (tempFile.exists()) {
            downloadedSize = tempFile.length();
        } else {
            try {
                if (!tempDir.exists()) {
                    tempDir.mkdirs();
                }
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            downloadedSize = 0;
        }
        return downloadedSize;
    }

    public void calculateStartPos(long fileLength, int threadNum) {
        if (startPos == 0) {
            startPos = threadId * fileLength / threadNum;
        }
    }

    public void calculateEndPos(long fileLength, int threadNum) {
        if (endPos == 0) {
            if (threadNum == threadId + 1) {
                endPos = fileLength - 1;
            } else {
                endPos = (threadId + 1) * fileLength / threadNum - 1;
            }
        }
    }
}
