package com.huxq17.download;

public class SpeedMonitor {
    private long totalRead = 0;
    private long lastSpeedCountTime = 0;
    final double NANOS_PER_SECOND = 1000000000.0;  //1秒=10亿nanoseconds
    final double BYTES_PER_MIB = 1024 * 1024;    //1M=1024*1024byte
    final double BYTES_PER_KB = 1024;
    final String BYTE_SUFFIX = "B/s";
    final String KB_SUFFIX = "KB/s";
    final String MIB_SUFFIX = "M/s";
    double speed = 0;
    String suffix = BYTE_SUFFIX;
    private TransferInfo downloadInfo;

    public SpeedMonitor(TransferInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
        downloadInfo.setSpeed(0 + suffix);
    }

    public void compute(int length) {
        totalRead += length;
        long curTime = System.nanoTime();
        if (lastSpeedCountTime == 0) {
            lastSpeedCountTime = curTime;
        }
        if (curTime >= lastSpeedCountTime + NANOS_PER_SECOND) {
            if (totalRead < BYTES_PER_KB) {
                speed = NANOS_PER_SECOND * totalRead / (curTime - lastSpeedCountTime);
                suffix = BYTE_SUFFIX;
            } else if (totalRead >= BYTES_PER_KB && totalRead < BYTES_PER_MIB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_KB / (curTime - lastSpeedCountTime);
                suffix = KB_SUFFIX;
            } else if (totalRead >= BYTES_PER_MIB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_MIB / (curTime - lastSpeedCountTime);
                suffix = MIB_SUFFIX;
            }
            downloadInfo.setSpeed(((double) Math.round(speed * 100) / 100) + suffix);
            lastSpeedCountTime = curTime;
            totalRead = 0;
        }
    }
}
