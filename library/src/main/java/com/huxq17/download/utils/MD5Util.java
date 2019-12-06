package com.huxq17.download.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

import static com.huxq17.download.utils.Util.closeQuietly;

public class MD5Util {
    private MD5Util() {
    }

    public static String getMD5(File file) {
        FileInputStream fileInputStream = null;
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            fileInputStream = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fileInputStream.read(buffer)) != -1) {
                MD5.update(buffer, 0, length);
            }
            return bytesToHexString(MD5.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            closeQuietly(fileInputStream);
        }
    }

    public static String getMD5ByStr(String src) {
        try {
            MessageDigest MD5 = MessageDigest.getInstance("MD5");
            byte[] buffer = src.getBytes();
            MD5.update(buffer);
            return bytesToHexString(MD5.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder();
        if (src == null || src.length <= 0) {
            return "";
        }
        for (int i = 0; i < src.length; i++) {
            int v = src[i] & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
