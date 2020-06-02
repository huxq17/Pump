package com.huxq17.download.core;

public class SpeedMonitor {
    private long totalRead = 0;
    private long lastSpeedCountTime = 0;
    final double NANOS_PER_SECOND = 1000000000.0;  //1秒=10亿nanoseconds
    final double BYTES_PER_GB = 1024 * 1024 * 1024;    //1G=1024*1024*1024byte
    final double BYTES_PER_MIB = 1024 * 1024;    //1M=1024*1024byte
    final double BYTES_PER_KB = 1024;
    final String BYTE_SUFFIX = "B/s";
    final String KB_SUFFIX = "KB/s";
    final String MIB_SUFFIX = "MB/s";
    final String GB_SUFFIX = "GB/s";
    double speed = 0;
    String suffix = BYTE_SUFFIX;

    public void download(long length) {
        totalRead += length;
        if (lastSpeedCountTime == 0) {
            lastSpeedCountTime = currentTime();
        }
    }

    public String getSpeed() {
        long curTime = currentTime();
        if (curTime >= lastSpeedCountTime + NANOS_PER_SECOND) {
            if (totalRead < BYTES_PER_KB) {
                speed = NANOS_PER_SECOND * totalRead / (curTime - lastSpeedCountTime);
                suffix = BYTE_SUFFIX;
            } else if (totalRead >= BYTES_PER_KB && totalRead < BYTES_PER_MIB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_KB / (curTime - lastSpeedCountTime);
                suffix = KB_SUFFIX;
            } else if (totalRead >= BYTES_PER_MIB && totalRead < BYTES_PER_GB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_MIB / (curTime - lastSpeedCountTime);
                suffix = MIB_SUFFIX;
            } else if (totalRead >= BYTES_PER_GB) {
                speed = NANOS_PER_SECOND * totalRead / BYTES_PER_GB / (curTime - lastSpeedCountTime);
                suffix = GB_SUFFIX;
            }
            lastSpeedCountTime = curTime;
            totalRead = 0;
        }
        if (Math.round(speed * 100) % 100 == 0) {
            return Math.round(speed * 100) / 100 + suffix;
        }
        return Math.round(speed * 100) / 100d + suffix;
    }

    public long currentTime() {
        return System.nanoTime();
    }
}
