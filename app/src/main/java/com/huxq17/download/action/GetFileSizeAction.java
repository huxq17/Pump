package com.huxq17.download.action;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class  GetFileSizeAction {
    public long proceed(String url) {
        HttpURLConnection conn = null;
        try {
            URL httpUrl = new URL(url);
            conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            String contentLengthStr = conn.getHeaderField("content-length");
            return Long.parseLong(contentLengthStr);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } finally {
            conn.disconnect();
        }
        return -1;
    }
}
