package com.huxq17.download.action;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GetFileSizeAction {
    public long proceed(String url) {
        HttpURLConnection conn = null;
        try {
            URL httpUrl = new URL(url);
            conn = (HttpURLConnection) httpUrl.openConnection();
            conn.setInstanceFollowRedirects(true);
            conn.setRequestMethod("HEAD");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            if (conn.getResponseCode() == 302) {
                httpUrl = new URL(conn.getHeaderField("location"));
                conn = (HttpURLConnection) httpUrl.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("HEAD");
                conn.setRequestProperty("Accept-Encoding", "identity");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
            }
            Map<String, List<String>> headers = conn.getHeaderFields();
            Set<Map.Entry<String, List<String>>> sets = headers.entrySet();
            for (Map.Entry<String, List<String>> entry : sets) {
                String key = entry.getKey();
                if (entry.getValue() != null)
                    for (String value : entry.getValue()) {
//                        Log.e("tag", "head key=" + key + ";value=" + value);
                    }
            }
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
