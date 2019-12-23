package com.huxq17.download.demo;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.text.DecimalFormat;

public class Util {
    public static String getDataSize(long size) {
        if (size < 0) {
            size = 0;
        }
        DecimalFormat format = new DecimalFormat("####.00");
        if (size < 1024) {
            return size + "bytes";
        } else if (size < 1024 * 1024) {
            float kbsize = size / 1024f;
            return format.format(kbsize) + "KB";
        } else if (size < 1024 * 1024 * 1024) {
            float mbsize = size / 1024f / 1024f;
            return format.format(mbsize) + "MB";
        } else if (size < 1024 * 1024 * 1024 * 1024) {
            float gbsize = size / 1024f / 1024f / 1024f;
            return format.format(gbsize) + "GB";
        } else {
            return "size: error";
        }

    }

    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;

        String var3;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];

            int length;
            while((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }

            String var5 = bytesToHexString(MD5.digest());
            return var5;
        } catch (Exception var9) {
            var9.printStackTrace();
            var3 = "";
        } finally {
            com.huxq17.download.utils.Util.closeQuietly(fileInputStream);
        }

        return var3;
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src != null && src.length > 0) {
            for(int i = 0; i < src.length; ++i) {
                int v = src[i] & 255;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }

                stringBuilder.append(hv);
            }

            return stringBuilder.toString();
        } else {
            return "";
        }
    }
}
